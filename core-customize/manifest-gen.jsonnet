local webapp(extension, path=null) = {
  name: extension,
  contextPath: if path == null then '/' + extension else path,
};

local storefrontExtension = 'demoshopstorefront';
local storefrontContextRoot = '';
local storefrontAddons = [
  'captchaaddon',
  'commerceorgsamplesaddon',
  'promotionenginesamplesaddon',
  'assistedservicestorefront',
  'assistedservicecustomerinterestsaddon',
  'assistedserviceyprofileaddon',
  'assistedservicepromotionaddon',
  'customerticketingaddon',
  'textfieldconfiguratortemplateaddon',
  'smarteditaddon',
  'consignmenttrackingaddon',
  'notificationaddon',
  'customerinterestsaddon',
  'stocknotificationaddon',
  'orderselfserviceaddon',
  'adaptivesearchsamplesaddon',
  'configurablebundleaddon',
  'pcmbackofficesamplesaddon',
  'xyformssamples',
  'xyformsstorefrontcommons',
  'personalizationsearchsamplesaddon',
  'personalizationyprofilesampledataaddon',
  'personalizationaddon',
  'profiletagaddon',
  'merchandisingaddon',
  'merchandisingstorefrontsampledataaddon',
  'spartacussampledataaddon',
];

local occExtension = 'ycommercewebservices';
local occAddons = [
  'cmsoccaddon',
  'acceleratorwebservicesaddon',

  'consignmenttrackingoccaddon',
  'customerinterestsoccaddon',
  'notificationoccaddon',
];

local smartEditWebapps = [
  // https://help.sap.com/viewer/1be46286b36a4aa48205be5a96240672/SHIP/en-US/7d3f83250d9846518f4154cfb18ae051.html
  webapp('oauth2', '/authorizationserver'),
  webapp('smartedit'),
  webapp('cmssmartedit'),
  webapp('cmssmarteditwebservices'),
  webapp('smarteditwebservices'),
  webapp('cmswebservices'),
  webapp('permissionswebservices'),
  webapp('previewwebservices'),

  webapp('personalizationsmartedit'),
  webapp('personalizationwebservices'),
  webapp('personalizationsearchsmartedit'),
  webapp('personalizationpromotionssmartedit'),

  // https://help.sap.com/viewer/50c996852b32456c96d3161a95544cdb/1905/en-US/b21fa18f635d49eaa4d9ce5997a6a814.html
  webapp('merchandisingsmartedit'),
];

{
  commerceSuiteVersion: '1905',
  useCloudExtensionPack: true,
  extensions: [
    'modeltacceleratorservices',
  ],
  useConfig: {
    properties: [
      {
        location: 'hybris/config/environments/common.properties',
      },
      {
        location: 'hybris/config/environments/accstorefront.properties',
        aspect: 'accstorefront',
      },
      {
        location: 'hybris/config/environments/api.properties',
        aspect: 'api',
      },
      {
        location: 'hybris/config/environments/backoffice.properties',
        aspect: 'backoffice',
      },
    ],
    extensions: {
      location: 'hybris/config/localextensions.xml',
      exclude: [],
    },
    solr: {
      location: 'solr',
    },
  },
  properties: [],
  storefrontAddons: [
    { addon: addon, storefront: storefrontExtension, template: 'yacceleratorstorefront' }
    for addon in storefrontAddons
  ] + [
    { addon: addon, storefront: occExtension, template: 'ycommercewebservices' }
    for addon in occAddons
  ],
  aspects: [
    {
      name: 'backoffice',
      webapps: [
        webapp('hac'),
        webapp('mediaweb', '/medias'),
        webapp('backoffice'),
        webapp('odata2webservices'),
      ] + smartEditWebapps,
    },
    {
      name: 'accstorefront',
      properties: [],
      webapps: [
        webapp(storefrontExtension, storefrontContextRoot),
        webapp('mediaweb', '/medias'),

        webapp('orbeonweb', '/web-orbeon'),
        webapp('xyformsweb'),
      ],
    },
    {
      name: 'backgroundProcessing',
      properties: [],
      webapps: [
        webapp('hac'),
        webapp('mediaweb', '/medias'),
      ],
    },
    {
      name: 'api',
      properties: [],
      webapps: [
        webapp(occExtension, '/rest'),
        // only necessaary for checkout/payment simulation?
        // https://help.sap.com/viewer/4c33bf189ab9409e84e589295c36d96e/1905/en-US/8abddeed86691014be559318fab13d44.html?q=acceleratorservices
        webapp('acceleratorservices'),
        webapp('oauth2', '/authorizationserver'),
        webapp('mediaweb', '/medias'),
      ],
    },
  ],
}
