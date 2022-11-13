FROM mcr.microsoft.com/mssql-tools

#https://learn.microsoft.com/en-us/sql/tools/sqlpackage/sqlpackage-download?view=sql-server-ver16
RUN apt-get update && apt-get install -y unzip libunwind8 && \
    UBUNTU_VERSION=$(cat /etc/lsb-release | grep DISTRIB_RELEASE= | sed "s/DISTRIB_RELEASE=//g" | awk -F"." '{print $1}') && \
    if [ $UBUNTU_VERSION -eq 16 ]; then apt-get install -y libicu55; elif [ $UBUNTU_VERSION -eq 17 ]; then apt-get install -y libicu57; \
    elif [ $UBUNTU_VERSION -eq 18 ]; then apt-get install -y libicu60; elif [ $UBUNTU_VERSION -eq 20 ]; then apt-get install -y libicu66; fi && \
    curl -s -L -o /opt/sqlpackage.zip https://aka.ms/sqlpackage-linux && mkdir /opt/sqlpackage && unzip /opt/sqlpackage.zip -d /opt/sqlpackage && \
    rm -f /opt/sqlpackage.zip && chmod a+x /opt/sqlpackage/sqlpackage && echo "export PATH=\"\$PATH:$/opt/sqlpackage\"" >> ~/.bashrc
