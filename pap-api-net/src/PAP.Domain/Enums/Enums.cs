namespace PAP.Domain.Enums;

public enum UserType { Local, Email }
public enum UserStatus { Active, Inactive }
public enum DomainStatus { Active, Inactive }
public enum RuleEffect { Permit, Deny }
public enum TestDecision { Permit, Deny, NotApplicable, Indeterminate }
public enum MatchFunction { Equals, Contains, GreaterThan, LessThan, Regex }
public enum RuleCombiningAlgorithm { PermitOverrides, DenyOverrides, FirstApplicable }
public enum ObligationFulfillOn { Permit, Deny, Both }
public enum AttributeDataType { String, Integer, Boolean, Double, Date, Time, DateTime, AnyUri }
