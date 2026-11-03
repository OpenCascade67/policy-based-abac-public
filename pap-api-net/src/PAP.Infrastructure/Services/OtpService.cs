using PAP.Application.Interfaces;

namespace PAP.Infrastructure.Services;

public class OtpService : IOtpService
{
    private static readonly Dictionary<string, (string Code, DateTime Expiry)> _otps = new();
    private static readonly object _lock = new();

    public string GenerateOtp(string email)
    {
        var code = new Random().Next(100000, 999999).ToString();
        lock (_lock)
        {
            _otps[email.ToLowerInvariant()] = (code, DateTime.UtcNow.AddMinutes(5));
        }
        // TODO: send code via email in production
        return code;
    }

    public bool VerifyOtp(string email, string code)
    {
        lock (_lock)
        {
            var key = email.ToLowerInvariant();
            if (!_otps.TryGetValue(key, out var stored)) return false;
            if (DateTime.UtcNow > stored.Expiry) { _otps.Remove(key); return false; }
            return stored.Code == code;
        }
    }

    public void ClearOtp(string email)
    {
        lock (_lock) { _otps.Remove(email.ToLowerInvariant()); }
    }
}
