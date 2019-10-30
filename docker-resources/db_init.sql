exec sp_configure 'contained database authentication', 1
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
