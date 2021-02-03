EXEC sp_configure 'show advanced options', 1;
GO
RECONFIGURE
GO
EXEC sp_configure 'cost threshold for parallelism', 50;
GO
RECONFIGURE
GO
EXEC sp_configure 'max degree of parallelism', 8;
GO
RECONFIGURE
GO
exec sp_configure 'contained database authentication', 1;
go
reconfigure
go
create database localdev containment = partial;
go
ALTER DATABASE localdev SET READ_COMMITTED_SNAPSHOT ON
go
ALTER DATABASE localdev SET ALLOW_SNAPSHOT_ISOLATION ON
GO
USE localdev
GO
CREATE USER localdev
WITH PASSWORD = 'localdev1!'
GO
exec sp_addrolemember 'db_owner', 'localdev'
