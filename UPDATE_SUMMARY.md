# Recruitment System - Update Summary

## Date
2026-05-16

## Overview
This update includes UI/UX improvements, bug fixes, and new features for the TA Recruitment System.

---

## 1. Insights Dialog (30-Day Hiring Insights)

### Issues Fixed
- **LoadException Error**: Fixed `CategoryAxis` with invalid `start="1"` attribute in `insights-dialog.fxml:59`
- **Chart Visibility**: Changed Applications by Module from BarChart to scrollable list view

### New Features
- **Module Statistics List View**
  - Format: `Module Name    ████████ 15 applications`
  - Scrollable list for all modules
  - Sorting by Name or Count buttons

### Files Modified
- `src/main/resources/fxml/insights-dialog.fxml`
- `src/main/java/com/bupt/tarecruit/controller/InsightsDialogController.java`

---

## 2. Post/Edit Job Tab

### Changes
- **Notes Field**: Increased from 4 to 8 rows (`prefRowCount="8"`)
- **Layout Redesign**: Organized into sections (Basic Information, Job Details)
- **Scroll Support**: Added ScrollPane for better navigation
- **Date Pickers**: Arranged horizontally (Start/End date side by side)
- **Form Status Label**: Moved to top section for better visibility

### Files Modified
- `src/main/resources/fxml/mo-dashboard-view.fxml`

---

## 3. TA Admin Tab - Space Optimization

### Changes
- **Removed VBox.vgrow="ALWAYS"**: Fixed excessive spacing
- **Both Tables**: Now share available space evenly
- **Reduced Padding**: Changed from 16px to 12px
- **Optimized Column Widths**: Better space utilization

### Files Modified
- `src/main/resources/fxml/mo-dashboard-view.fxml`

---

## 4. Password Management - Separate Dialog

### New Feature
- **Independent Password Dialog**: Moved from inline form to popup dialog
- **Dedicated Button**: "Change Password" button in profile page
- **Supports Both MO and TA Users**: Single dialog for both user types

### New Files Created
- `src/main/resources/fxml/change-password-dialog.fxml`
- `src/main/java/com/bupt/tarecruit/controller/ChangePasswordDialogController.java`

### Files Modified
- `src/main/resources/fxml/mo-dashboard-view.fxml` (Removed inline password fields)
- `src/main/resources/fxml/ta-dashboard-view.fxml` (Removed inline password fields)
- `src/main/java/com/bupt/tarecruit/controller/MoDashboardController.java`
- `src/main/java/com/bupt/tarecruit/controller/TaDashboardController.java`

---

## 5. Profile Pages - Redesign

### MO Profile Tab
- **Removed maxWidth constraint**: Better space utilization
- **Section-based Layout**: Account Info, Contact Info, Professional Info
- **Scrollable Content**: Added ScrollPane
- **Larger Text Areas**: Increased rows for better input

### TA Profile Tab
- **Similar Section-based Layout**
- **Improved CV Section**: Better button arrangement
- **Increased TextArea Rows**: Skills (3), Experience (4), Self-eval (4)
- **Max Width**: Expanded to 750px

### Files Modified
- `src/main/resources/fxml/mo-dashboard-view.fxml`
- `src/main/resources/fxml/ta-dashboard-view.fxml`

---

## 6. Applicants Search - Enhancement

### Issues Fixed
- **UI Not Updating**: `filterApplicants()` now calls `renderApplicantCards()`
- **Case Sensitivity**: All search fields now case-insensitive
- **AI Top 3 Bug**: Fixed search after using AI Top 3 filter

### New Features
- **Major Field Search**: Can now search by student's major
- **Clear Button**: Added button to clear search quickly
- **Real-time Filtering**: Instant results as you type

### Search Fields (All Case-Insensitive)
- TA ID
- Name
- Phone
- Email
- Status (Pending/Hired/Rejected/Withdrawn)
- Major (NEW)

### Files Modified
- `src/main/resources/fxml/mo-dashboard-view.fxml`
- `src/main/java/com/bupt/tarecruit/controller/MoDashboardController.java`

---

## File Changes Summary

### Modified Files
| File | Lines Changed | Description |
|------|---------------|-------------|
| `application.css` | +295 | Style enhancements |
| `MoDashboardController.java` | Major refactor | Search, dialog handling |
| `TaDashboardController.java` | +67 | Password dialog support |
| `mo-dashboard-view.fxml` | +269 | Layout redesign |
| `ta-dashboard-view.fxml` | +119 | Profile redesign |
| `insights-dialog.fxml` | Modified | List view instead of chart |
| `InsightsDialogController.java` | New/Modified | Stats list rendering |

### New Files Created
- `change-password-dialog.fxml`
- `ChangePasswordDialogController.java`
- `InsightsDialogController.java` (was previously broken)
- `insights-dialog.fxml` (fixed version)

### Total Changes
- **10 files changed**
- **1256 insertions(+)**
- **639 deletions(-)**

---

## Testing Checklist

- [x] Compilation successful
- [x] Insights dialog opens without errors
- [x] Module stats display correctly with sorting
- [x] Password dialog works for both MO and TA
- [x] Search filters work correctly
- [x] Profile pages display properly
- [x] Notes field displays more content

---

## Known Issues
None at this time.

---

## Future Improvements
- Consider adding pagination for large applicant lists
- Add export functionality for insights data
- Implement search history
