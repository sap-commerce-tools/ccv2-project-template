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

#https://learn.microsoft.com/en-us/sql/tools/sqlpackage/sqlpackage-download?view=sql-server-ver16
apt-get update && apt-get install -y unzip libunwind8 libicu55 

cd ~
curl -s -L -o sqlpackage.zip https://aka.ms/sqlpackage-linux
mkdir ~/sqlpackage
unzip sqlpackage.zip -d ~/sqlpackage 
echo "export PATH=\"\$PATH:$HOME/sqlpackage\"" >> ~/.bashrc
chmod a+x ~/sqlpackage/sqlpackage
source ~/.bashrc

echo "SQL Server is up; Starting bacpac import..."
# Run the setup script to create the DB and the schema in the DB
/root/sqlpackage/sqlpackage /a:Import /sf:/tmp/resources/backup.bacpac /TargetServerName:mssql /TargetDatabaseName:localdev /TargetUser:sa /TargetPassword:$SA_PASSWORD