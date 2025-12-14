Quick reference for removing `permit-completely` migration code in a future version.

**Flags.java**
- `PERMIT_COMPLETELY` field and `@Deprecated` annotation

**WorldGuardExtraFlagsPlusPlugin.java**
- `flagRegistry.register(Flags.PERMIT_COMPLETELY);` in `onLoad()`
- `migrateRegionFiles()` method (~line 234-305)
- `updateInMemoryRegions()` method (~line 311-360)
- Migration calls in `onEnable()` (~lines 135, 142-149)

**EntityListener.java**
- Fallback to `PERMIT_COMPLETELY` in `isBlocked()` (~lines 90-112)
- Fallback to `permit-completely-blocked` message in `sendBlocked()` (~lines 133-137)

**Documentation**
- Remove `permit-completely` mentions from README.md and .media files


After 2-3 major versions, once most servers have migrated. The migration runs automatically on plugin load, so old flag usage should be minimal by then.
