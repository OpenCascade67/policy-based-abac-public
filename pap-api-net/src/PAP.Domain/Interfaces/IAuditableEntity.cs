namespace PAP.Domain.Interfaces;

public interface IAuditableEntity
{
    DateTime CreatedAt { get; set; }
    Guid? CreatedById { get; set; }
    DateTime LastModifiedAt { get; set; }
    Guid? LastModifiedById { get; set; }
}
