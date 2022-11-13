#!/bin/bash

# Wait $TIMEOUT seconds for SQL Server to start up by ensuring that 
# calling SQLCMD does not return an error code, which will ensure that sqlcmd is accessible
# and that system and user databases return "0" which means all databases are in an "online" state
# https://docs.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-databases-transact-sql?view=sql-server-2017 

DBSTATUS=1
ERRCODE=1
i=0
TIMEOUT=120

while [[ "${DBSTATUS:-1}" -ne "0" ]] && [[ "$i" -lt "$TIMEOUT" ]]; do
    i=$((i+1))
    DBSTATUS_STR="$(/opt/mssql-tools/bin/sqlcmd -h -1 -t 1 -S "$SQLSERVER" -U sa -P "$SA_PASSWORD" -Q "SET NOCOUNT ON; Select SUM(state) from sys.databases")"
    DBSTATUS=${DBSTATUS_STR//[ $'\001'-$'\037']}
    ERRCODE=$?
    sleep 1
done
if [ "$DBSTATUS" -ne "0" ] || [ "$ERRCODE" -ne 0 ]; then 
    echo "SQL Server took more than $TIMEOUT seconds to start up or one or more databases are not in an ONLINE state"
    exit 1
fi

if [ -f "/tmp/resources/snapshot.bacpac" ]; then
    echo "SQL Server is up; Starting snapshot import..."
    /opt/sqlpackage/sqlpackage /Action:Import /SourceFile:/tmp/resources/snapshot.bacpac /TargetTrustServerCertificate:true /TargetServerName:mssql /TargetDatabaseName:localdev /TargetUser:sa /TargetPassword:$SA_PASSWORD
else
    echo "No database snapshot found. Have you placed the snapshot.bacpac on the ./docker-resources folder ?"
    exit 1
fi
