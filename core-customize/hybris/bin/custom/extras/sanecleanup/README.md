# sanecleanup

![SAP Commerce 1811+](https://img.shields.io/badge/Commerce-1811+-0051ab?logo=SAP)

Sensible defaults for data retention and cleanup for SAP Commerce, based on my CX Works article [Data Maintenance and Cleanup][article]



1. Download the repository as zip file
1. Unpack to `hybris/bin/custom`
1. **Review and adapt the retention rules** and cronjobs defined in `sanecleanup/resources/impex/*.impex`\
1. If possible, disable storing of saved values / change history too! ([help.sap.com][stored], further recommendations in my [article][stored-kill])
1. Add extension to your `localextensions.xml`

    ````xml
   <extension name="sanecleanup" />
    ````

1. Build and deploy.\
  (The rules will be imported during system update)

**Warning**\
The first run of `cronJobLogCleanupCronJob` will take a _very_ long time, if you have never removed any cronjob log files (type `LogFile`).\
Please consider importing and executing the script job defined in [bulkdelete-cronjoblogs.impex](resources/impex/bulkdelete-cronjoblogs.impex) **before** you set up the automated cleanup!\
The job will remove all log files except the five most recent ones per CronJob.
(Disclaimer: the script was tested on MS SQL / Azure SQL. It is not guaranteed to work for other Databases)

## Support 

Please open an [issue] describing your problem or your feature request.

## Contributing

Any and all pull requests are welcome.\
Please describe your change and the motiviation behind it.

[issue]: https://github.com/sap-commerce-tools/sanecleanup/issues
[article]: https://www.sap.com/cxworks/article/456895555/data_maintenance_and_cleanup
[one]: https://www.sap.com/cxworks/article/456895555/data_maintenance_and_cleanup#DataMaintenanceandCleanup-One-timeCleanUp
[stored]: https://help.sap.com/viewer/d0224eca81e249cb821f2cdf45a82ace/LATEST/en-US/076cde47206048b9ada3fa0d336c1060.html
[stored-kill]: https://www.sap.com/cxworks/article/456895555/data_maintenance_and_cleanup#DataMaintenanceandCleanup-SavedValues
