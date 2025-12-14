Quick reference for removing `permit-completely` migration code in a future version.

**Status:** `permit-completely` flag has been removed in version 4.3.8. Migration code is kept for one more version to help migrate existing region files.

**Flags.java**
- ✅ REMOVED: `PERMIT_COMPLETELY` field (removed in 4.3.8)

**WorldGuardExtraFlagsPlusPlugin.java**
- ✅ REMOVED: `flagRegistry.register(Flags.PERMIT_COMPLETELY);` (removed in 4.3.8)
- `migrateRegionFiles()` method (lines ~252-310) - KEEP for one more version
- `updateInMemoryRegions()` method (lines ~333-385) - Simplified, no longer migrates in-memory
- Migration calls in `onEnable()` (lines ~141, 152-158) - KEEP for one more version

**EntityListener.java**
- ✅ REMOVED: Fallback to `PERMIT_COMPLETELY` in `isBlocked()` method (removed in 4.3.8)
- ✅ REMOVED: Deprecation warning logging (removed in 4.3.8)
- ✅ REMOVED: Fallback to `permit-completely-blocked` message in `sendBlocked()` method (removed in 4.3.8)

**Messages.java**
- Old `messages.yml` deletion logic in `initialize()` method (lines ~35-59)
  - Check for old `messages.yml` file
  - Delete if it contains `permit-completely-blocked` key
- Message key migration in `reloadMessages()` method (lines ~137-157)
  - Migration of `permit-completely-blocked` to `disable-completely-blocked`
  - Text-based replacement to preserve formatting
  - Comment update from "Permit completely" to "Disable completely"
- `inventory-craft-blocked` auto-add logic in `reloadMessages()` method (lines ~159-264)
  - Check if key is missing
  - Add with default value if missing
  - Quote style detection and text-based insertion

**Documentation**
- Remove `permit-completely` mentions from README.md and .media files
- Remove deprecation notes about `permit-completely` flag

**Note:**
After 2-3 major versions, once most servers have migrated. The migration runs automatically on plugin load, so old flag usage should be minimal by then.
