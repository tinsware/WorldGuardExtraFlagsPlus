Quick reference for removing `permit-completely` migration code in a future version.

**Flags.java**
- `PERMIT_COMPLETELY` field and `@Deprecated` annotation (line ~82)

**WorldGuardExtraFlagsPlusPlugin.java**
- `flagRegistry.register(Flags.PERMIT_COMPLETELY);` in `onLoad()` (line ~101)
- `migrateRegionFiles()` method (lines ~252-310)
- `updateInMemoryRegions()` method (lines ~333-385)
- Migration calls in `onEnable()` (lines ~141, 152-158)

**EntityListener.java**
- Fallback to `PERMIT_COMPLETELY` in `isBlocked()` method (lines ~137-157)
- Deprecation warning logging (lines ~143-156)
- Fallback to `permit-completely-blocked` message in `sendBlocked()` method (line ~181)

**Documentation**
- Remove `permit-completely` mentions from README.md and .media files
- Remove deprecation notes about `permit-completely` flag

**Note:**
After 2-3 major versions, once most servers have migrated. The migration runs automatically on plugin load, so old flag usage should be minimal by then.
