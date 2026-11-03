**Build a responsive, enterprise‑grade web application that acts as a Policy Administration Point (PAP) for Attribute‑Based Access Control (ABAC).**

> The application must be **web‑based**, **standards‑aligned**, and suitable for **enterprise and government users**, following:
>
> *   Australian Digital Transformation Agency (DTA) Web & Accessibility Standards (WCAG 2.1 AA)
> *   Google Material Design system
> *   Clean, professional, and elegant visual style
> *   Responsive design supporting desktop, tablet, and mobile
>
> Use a **collapsible left sidebar menu** and a **top application header**.

***

## Global UX & Architecture Requirements

*   Reactive UI (desktop, tablet, mobile)
*   Google Material components (buttons, tabs, cards, dialogs, drawers)
*   Accessibility:
    *   Keyboard navigable
    *   Screen‑reader friendly
    *   Proper labels, aria attributes, contrast ratios
*   Use clear information hierarchy
*   Use consistency for slide‑out panels and grids
*   Assume all data is mocked or stored locally (local storage / in‑memory for demo)

***

## Authentication – Login Page

### Login Options

Provide **two login methods on separate tabs**, clearly labelled with instructions:

#### 1. Username & Password Login

*   Username and password are **predefined and stored in local storage**
*   Input validation and error messaging
*   Login button enabled only when fields are valid

#### 2. Email One‑Time Code Login

*   User enters a valid email address
*   System simulates sending a **one‑time authentication code**
*   User enters the code to authenticate
*   On first login via email, the user record is created and stored

***

## Top Header – User Profile Access

*   After login, show **user profile icon + username** in the top‑right corner
*   Clicking opens the **User Profile Panel**

***

## User Profile Panel (Slide‑down / Popover)

The panel must:

*   Show **current active domain**
*   Show **user roles in the current domain**
*   Allow switching between domains the user belongs to
*   Allow setting a **default domain** (used automatically on next login)
*   If the user has **no associated domains**, show a clear informational message
*   Include a **Logout** action

***

## User Profile Page

Accessible only to the currently logged‑in user.

### Tabs

1.  **Basic Information**
    *   Username
2.  **Domain Information**
    *   List of all domains associated with the user
    *   For each domain, list the roles assigned

***

## Sidebar Navigation Structure

### Section 1: Administration

*   Manage User Roles
*   Manage Domains
*   Manage Users

### Section 2: Access Control Policies

*   Manage Access Control Policies
*   Test Access Control Policies

### Section 3: User Profile

*   Profile

### Section 4: Application Settings

*   App Settings

Sidebar must be collapsible to icons only.

***

## Manage User Roles Page

### Access Control

*   Only authorised users can access or see this menu item

### Functionality

*   Display a **grid of user roles** with:
    *   Role name
    *   Description
    *   Associated permissions

### Default System Roles (non‑editable, non‑deletable)

*   System Admin
*   Domain Admin
*   Policy Author
*   Policy Viewer

### Role Capabilities

*   **System Admin**
    *   Predefine usernames and passwords
    *   Create, edit, delete domains
    *   Create custom user roles
*   **System Admin & Domain Admin**
    *   Assign users to roles within domains
*   **Policy Author**
    *   Create, edit, view, test policies
    *   Manage ABAC scenarios
*   **Policy Viewer**
    *   View and test policies
    *   Manage ABAC scenarios

### Custom Roles

*   Only user‑created roles are editable and deletable

### Create/Edit Role

*   Slide‑out panel from the right
*   Partially covers screen on desktop, fully on mobile
*   Fields:
    *   Role name (text field)
    *   Description (text area)

***

## Manage Domains Page

### Access Control

*   Only authorised users can access

### Grid

*   Domain name
*   Description
*   Status (Active / Inactive)

### Create/Edit Domain

*   Slide‑out panel
*   Fields:
    *   Domain name
    *   Domain description
    *   Toggle to activate / deactivate domain
*   When a domain is inactive, **all domain‑related functionality becomes unavailable**

***

## Manage Users Page

### Access Control

*   Only authorised users can access

### Grid (Paginated, Sorted by Username)

*   Username
*   User type (Predefined / Email login)
*   Status (Active / Inactive)
*   Associated domains
*   Created date/time
*   Last modified date/time
*   Actions: Edit, Delete

### Features

*   Fuzzy search by username
*   Users come from:
    *   Predefined users
    *   Email‑login users recorded on first login

### Create User

*   Slide‑out form
*   Username
*   Password

### Edit User

*   Predefined user:
    *   Update password
    *   Activate / deactivate
*   Email‑login user:
    *   Activate / deactivate only
*   All users:
    *   Associate with one or more domains
    *   Assign one or more roles per domain

***

## Manage Access Control Policies Page

### Access Control

*   Only authorised users can access

### Grid (Paginated)

*   Policy name
*   Description
*   Created time
*   Last modified time
*   Actions: Edit, Delete

### Features

*   Keyword search
*   Create policy → navigates to Policy Creation Page
*   Edit policy → same UI, pre‑populated

### Policy Creation Page

*   Policy name
*   Description
*   **Policy Editor control**

#### Policy Editor Requirements

*   Visual rule editor aligned with **OASIS XACML 3.0**
*   Supports:
    *   Policy / Rule structure
    *   Effects (Permit / Deny)
    *   Targets (Subject, Resource, Action, Environment)
    *   Boolean conditions (AND / OR)
*   Output must be mappable to:
    *   XACML XML
    *   XACML JSON Profile
*   Maintain a structured internal representation

***

## ABAC Scenario Page

### Access Control

*   Only authorised users

### Grid

*   Scenario name
*   Description
*   Associated test cases
*   Created time
*   Last modified time
*   Actions: Edit, Delete

### Create/Edit Scenario

*   Fields:
    *   Name
    *   Description
    *   Value (JSON editor or large text area)
*   Static list of associated test cases

***

## Test Access Control Policy Page

### Access Control

*   Only authorised users

### Grid

*   Test case name
*   Description
*   Policy (All or specific policy)
*   Created time
*   Last modified time
*   Actions: Edit, Delete

### Create/Edit Test Case Flow

1.  Enter test case name and description
2.  Choose:
    *   Test all policies
    *   Test a single policy
3.  If single policy:
    *   Enable typeahead policy selector
4.  Select ABAC scenario (optional)
    *   Typeahead dropdown
    *   Selected scenario value populates JSON editor
5.  Or manually enter ABAC input JSON
6.  Enable **Run Test** when input is present
7.  Display test result at bottom of page
8.  Allow saving test case at any time

***

## Application Settings Page

*   Toggle light / dark mode
*   Select color scheme

***

## Visual Tone

*   Enterprise‑grade
*   Professional
*   Minimalist
*   Confident but neutral
*   Suitable for government and regulated industries
