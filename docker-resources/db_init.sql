EXEC sp_configure 'show advanced options', 1;
GO
RECONFIGURE
GO
EXEC sp_configure 'cost threshold for parallelism', 50;
EXEC sp_configure 'max degree of parallelism', 4;
EXEC sp_configure 'contained database authentication', 1;
GO
RECONFIGURE
GO
USE master;
GO
IF DB_ID (N'localdev') IS NULL
BEGIN
    CREATE DATABASE localdev CONTAINMENT = PARTIAL;
    ALTER DATABASE localdev SET READ_COMMITTED_SNAPSHOT ON;
    ALTER DATABASE localdev SET ALLOW_SNAPSHOT_ISOLATION ON;
END
ELSE
BEGIN
    PRINT N'DB localdev already exists';
END
GO
USE localdev;
GO
IF DATABASE_PRINCIPAL_ID(N'localdev') IS NULL
BEGIN
    CREATE USER localdev WITH PASSWORD = 'localdev1!';
END
ELSE
BEGIN
    PRINT N'user localdev already exists';
END
GO
EXEC sp_addrolemember 'db_owner', 'localdev';
GO
