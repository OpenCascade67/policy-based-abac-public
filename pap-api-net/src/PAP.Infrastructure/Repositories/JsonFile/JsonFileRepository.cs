using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using PAP.Application.Interfaces;
using PAP.Domain.Interfaces;

namespace PAP.Infrastructure.Repositories.JsonFile;

public class DataStoreOptions
{
    public string DataFolder { get; set; } = "data";
}

public abstract class JsonFileRepository<T> : IRepository<T> where T : class
{
    private readonly string _filePath;
    private readonly ICurrentUserService _currentUser;
    private static readonly SemaphoreSlim _lock = new(1, 1);

    private static readonly JsonSerializerOptions _jsonOptions = new()
    {
        WriteIndented = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        Converters = { new JsonStringEnumConverter(JsonNamingPolicy.CamelCase) },
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull
    };

    protected JsonFileRepository(IOptions<DataStoreOptions> options, string fileName, ICurrentUserService currentUser)
    {
        var folder = options.Value.DataFolder;
        _currentUser = currentUser;
        if (!Path.IsPathRooted(folder))
            folder = Path.Combine(AppContext.BaseDirectory, folder);
        Directory.CreateDirectory(folder);
        _filePath = Path.Combine(folder, fileName);
    }

    protected abstract Guid GetId(T entity);
    protected abstract void SetId(T entity, Guid id);

    protected async Task<List<T>> ReadRawAllAsync()
    {
        if (!File.Exists(_filePath)) return new List<T>();
        await _lock.WaitAsync();
        try
        {
            var json = await File.ReadAllTextAsync(_filePath);
            return JsonSerializer.Deserialize<List<T>>(json, _jsonOptions) ?? new List<T>();
        }
        finally { _lock.Release(); }
    }

    protected async Task<List<T>> ReadAllAsync()
    {
        var all = await ReadRawAllAsync();
        
        // Domain Filtering and Migration
        if (typeof(IDomainEntity).IsAssignableFrom(typeof(T)) && _currentUser.CurrentDomainId.HasValue)
        {
            var domainId = _currentUser.CurrentDomainId.Value;
            // Lenient Migration: If an entity has an empty DomainId, assign it to the 'System Default Domain'
            // (00000000-0000-0000-0000-000000000001). This ensures existing global data remains
            // isolated within the default domain and isn't "stolen" by other domains.
            var defaultDomainId = Guid.Parse("00000000-0000-0000-0000-000000000001");
            bool changed = false;
            foreach (var item in all.OfType<IDomainEntity>().Where(e => e.DomainId == Guid.Empty))
            {
                item.DomainId = defaultDomainId;
                changed = true;
            }
            
            return all.Where(e => ((IDomainEntity)e).DomainId == domainId).ToList();
        }

        return all;
    }

    protected async Task WriteAllAsync(List<T> entities)
    {
        await _lock.WaitAsync();
        try
        {
            var json = JsonSerializer.Serialize(entities, _jsonOptions);
            await File.WriteAllTextAsync(_filePath, json);
        }
        finally { _lock.Release(); }
    }

    public async Task<List<T>> GetAllAsync() => await ReadAllAsync();

    public async Task<T?> GetByIdAsync(Guid id)
        => (await ReadAllAsync()).FirstOrDefault(e => GetId(e) == id);

    public async Task<List<T>> GetAllOriginalAsync() => await ReadRawAllAsync();

    public async Task<T> AddAsync(T entity)
    {
        // Bypass ReadAllAsync filtering so we don't accidentally write back only the filtered list
        var all = await GetAllOriginalAsync();
        
        if (GetId(entity) == Guid.Empty) SetId(entity, Guid.NewGuid());
        
        if (entity is IDomainEntity de && _currentUser.CurrentDomainId.HasValue)
        {
            de.DomainId = _currentUser.CurrentDomainId.Value;
        }

        all.Add(entity);
        await WriteAllAsync(all);
        return entity;
    }

    public async Task<T> UpdateAsync(T entity)
    {
        // Bypass ReadAllAsync filtering
        var all = await GetAllOriginalAsync();
        
        var id = GetId(entity);
        var idx = all.FindIndex(e => GetId(e) == id);
        if (idx < 0) throw new KeyNotFoundException($"Entity {id} not found.");
        
        // Ensure DomainId is preserved or set
        if (entity is IDomainEntity de && de.DomainId == Guid.Empty && _currentUser.CurrentDomainId.HasValue)
        {
            de.DomainId = _currentUser.CurrentDomainId.Value;
        }

        all[idx] = entity;
        await WriteAllAsync(all);
        return entity;
    }

    public async Task DeleteAsync(Guid id)
    {
        var all = await GetAllOriginalAsync();
        var removed = all.RemoveAll(e => GetId(e) == id);
        if (removed == 0) throw new KeyNotFoundException($"Entity {id} not found.");
        await WriteAllAsync(all);
    }

    public async Task<bool> ExistsAsync(Guid id)
        => (await GetAllOriginalAsync()).Any(e => GetId(e) == id);
}
