Implement the following changes exactly. First analyze the current codebase and compare it with the old repository before making any changes. Do **not** introduce any new business logic unless explicitly mentioned. Follow the old flow wherever applicable while adapting it to the new project structure and new database schema.

---

# 1. Privilege Restrictions

## My Organization

* Remove **My Organization** completely from normal User privileges.
* Users must **never** receive the full My Organization module.

Only Admins can assign these three pages to their child users:

* Overview
* Hierarchy (User hierarchy only)
* User Status

These pages must expose **only user data**.

Never expose:

* Admin Status
* Banks & Branches
* My Queue

He can perform actions on users
Any admin actions
Block/Unblock
Replacement actions
Delegation actions
Scheduling actions

The old repository already has this behaviour. Reuse that flow and adapt it to the new schema.

---

# 2. Checker Visibility

If a role is a **Maker**, hide all Checker-related menus and privileges.

A normal user must never be able to create or assign a Checker role.

Only:

* Bank Admin
* Branch Admin

can create Checker users.

Reuse the old implementation wherever possible.

---

# 3. User Management Visibility

Remove **User Management** from user privileges.

It should remain an Admin-only module.

Users should only receive:

* User List
* View User

when granted.

---

# 4. Replacement & Delegation Screen

Rename the current menu to something more meaningful such as:

* User Lifecycle History
  or
* Replacement & Delegation History

This screen is **view only**.

Remove every action button:

* Replace
* Delegate
* Block
* Schedule
* Reactivate
* Edit

It should only display replacement/delegation history.

Use the existing backend tables.

---

# 5. Approval Request History

Create a new menu.

Example name:

* Approval Request History

This page should fetch data from:

RECON_APPROVAL_REQUEST

View only.

No action buttons.

---

# 6. Privilege Flow

Before loading menus, make these mandatory:

* Bank Type
* Product

Until both are selected:

* Do not load menu tree.
* Disable privilege assignment.

Bank Types must always be fetched dynamically.

Never hardcode them.

---

# 7. Role Validation

Remove global unique validation.

Roles are tenant scoped.

Examples:

Bank A
Maker

Bank B
Maker

Branch X
Maker

Branch Y
Maker

All are valid.

If the same Bank/Branch already contains the same role:

Show confirmation popup:

"This role already exists for this organization.
Do you still want to continue?"

Buttons:

* Yes
* No

Selecting Yes continues creation.

This flow already exists in the old repository. Reuse it.

---

# 8. Add Menu Flow

Menus already inserted manually in RECON_MENU_MASTER are only used for validation.

Do NOT modify them.

When Add Menu is used:

* Validate the menu exists in RECON_MENU_MASTER.
* If not found, show:

"This menu is not registered in the system."

When validation succeeds:

Append a new row into the same table containing:

* Product ID
* Process ID
* URL
* Mapping details

Do NOT overwrite the original seed rows.

Multiple mappings for the same menu must be supported.

Maker → Checker approval flow for newly added submenu mappings must remain exactly as before.

---

# 9. Privilege Visibility Rule

If a Master/Main menu has Submenus:

Do not display that Master/Main in Privileges until at least one Submenu is mapped to a Process ID.

Once even one Submenu is mapped:

Display:

* Master
* Main
* Submenu

If a Master/Main has no Submenu at all:

Display it normally.

---

# 10. Bank Onboarding

Adapt the complete Bank Onboarding UI from the old repository.

Keep:

* New Configuration design system
* New colors
* New tokens

But restore:

* Layout
* Width
* Page shell
* Spacing
* Overall form proportions

Also restore:

* Product Valid From
* Product Valid To

exactly like the old implementation.

---

# 11. Dynamic Bank Type & Product

Fetch dynamically for every Admin/User according to their organization.

Never hardcode values.

---

# 12. Verify Old Business Flows

Before changing anything, compare with the old repositories and ensure these flows remain identical while adapting them to the new schema:

* Replacement
* Delegation
* Approval
* Maker/Checker
* Notifications
* Email triggers
* Status changes
* Validation rules

Reuse the old business flow only.

Do not copy the old database structure.

Adapt the logic to the new tables.

---

# 13. Insert SQL

Prepare SQL for:

* Maker Dashboard
* Updated menu catalogue

Only include the required menus discussed.

Do not include obsolete routes or development-only pages.

---

Finally, before implementation, provide a short implementation plan explaining:

1. What files will change.
2. What backend changes are required.
3. What frontend changes are required.
4. Any backend API gaps or blockers.

Do not start coding until the implementation plan is complete and verified.
