// find . -name 'extensioninfo.xml' | \
// xargs xml sel -t -v '//extension/@name' -o ": '"  -v '//webmodule/@webroot' -o "'," -n | \
// grep -v "^\(.\+\): '/\1',$" | grep '/'

local nonstandard_context_paths = {
  mediaweb: '/medias',
  testweb: '/test',
  oauth2: '/authorizationserver',
  maintenanceweb: '/maintenance',
  commercewebservices: '/occ',
  ycommercewebservices: '/rest',
  scimwebservices: '/scim',
  orbeonweb: '/web-orbeon',
};

local webapp(extension, path=null) = {
  name: extension,
  contextPath: if path == null then
    if extension in nonstandard_context_paths then
      nonstandard_context_paths[extension]
    else
      '/' + extension
  else
    path,
};

// CONFIGURE YOUR MANIFEST HERE

local storefrontContextRoot = '';
local storefrontAddons = [
  'smarteditaddon',
  //TODO: add more addons as required here
  //      don't forget to add them to localextensions.xml too!
];

local smartEditWebapps = [
  // Activating SmartEdit for a Storefront
  // https://help.sap.com/viewer/1be46286b36a4aa48205be5a96240672/SHIP/en-US/7d3f83250d9846518f4154cfb18ae051.html
  // Default Smartedit webapps
  webapp('oauth2'),
  webapp('smartedit'),
  webapp('cmssmartedit'),
  webapp('cmssmarteditwebservices'),
  webapp('smarteditwebservices'),
  webapp('cmswebservices'),
  webapp('permissionswebservices'),
  webapp('previewwebservices'),
  //TODO: add/remove smartedit features here
  //      don't forget to add the extensions to localextensions.xml too!
  // // Smartedit personalization
  // webapp('personalizationsmartedit'),
  // webapp('personalizationwebservices'),
  // // Smartedit personalization promotion
  // webapp('personalizationpromotionssmartedit'),
  // // Smartedit personalization search
  // webapp('personalizationsearchsmartedit'),
  // // Smartedit promotion
  // webapp('merchandisingcmswebservices'),
  // // https://help.sap.com/viewer/50c996852b32456c96d3161a95544cdb/1905/en-US/b21fa18f635d49eaa4d9ce5997a6a814.html
  // webapp('merchandisingsmartedit'),
];

// ------------ MANIFEST ------------
function(intExtPackVersion=null, solrVersion=null, solrCustom='solr', accStorefrontEnabled=false, storefrontExtension=nulll, commerceVersion='2211.23') {
  commerceSuiteVersion: commerceVersion,
  extensionPacks: [
  ] + if intExtPackVersion != null then [
    {
      name: 'hybris-commerce-integrations',
      version: intExtPackVersion,
    },
  ] else [],
  extensions: [
    // modeltacceleratorservices is only available in CCv2 (not part of the cloud extension pack)
    // -> configure it in manifest.json
    // https://help.sap.com/viewer/0fa6bcf4736c46f78c248512391eb467/SHIP/en-US/b13c673497674994a7f243e3225af9b3.html
    'modeltacceleratorservices',
  ],
  useConfig: {
    properties: [
      {
        location: 'hybris/config/cloud/common.properties',
      },
      {
        location: 'hybris/config/cloud/aspect/api.properties',
        aspect: 'api',
      },
      {
        location: 'hybris/config/cloud/aspect/backoffice.properties',
        aspect: 'backoffice',
      },
      {
        location: 'hybris/config/cloud/aspect/backgroundprocessing.properties',
        aspect: 'backgroundProcessing',
      },
      {
        location: 'hybris/config/cloud/aspect/admin.properties',
        aspect: 'admin',
      },
      {
        location: 'hybris/config/cloud/persona/development.properties',
        persona: 'development',
      },
    ] + if accStorefrontEnabled then [
      {
        location: 'hybris/config/cloud/aspect/accstorefront.properties',
        aspect: 'accstorefront',
      },
    ] else [],
    extensions: {
      location: 'hybris/config/localextensions.xml',
      exclude: [],
    },
  } + if solrVersion != null && solrCustom != null then {
    solr: {
      location: solrCustom,
    },
  }
  else {},
  properties: [],
  storefrontAddons: [] + if accStorefrontEnabled then [
    { addon: addon, storefront: storefrontExtension, template: 'yacceleratorstorefront' }
    for addon in storefrontAddons
  ] else [],
  aspects: [
    {
      name: 'backoffice',
      webapps: [
        webapp('hac'),
        webapp('mediaweb'),
        webapp('backoffice'),
        webapp('odata2webservices'),
      ] + smartEditWebapps,
    },
    {
      name: 'backgroundProcessing',
      properties: [],
      webapps: [
        webapp('hac'),
        webapp('mediaweb'),
      ],
    },
    {
      name: 'api',
      properties: [],
      webapps: [
        webapp('commercewebservices'),
        // only necessary for checkout/payment mocks
        // https://help.sap.com/viewer/4c33bf189ab9409e84e589295c36d96e/latest/en-US/8abddeed86691014be559318fab13d44.html
        webapp('acceleratorservices'),
        webapp('oauth2'),
        webapp('mediaweb'),
      ],
    },
  ] + if accStorefrontEnabled then [
    {
      name: 'accstorefront',
      properties: [],
      webapps: [
        webapp(storefrontExtension, storefrontContextRoot),
        webapp('mediaweb'),
      ],
    },
  ] else [],
} + if solrVersion != null then {
  // https://help.sap.com/viewer/1be46286b36a4aa48205be5a96240672/latest/en-US/b35bc14a62aa4950bdba451a5f40fc61.html#loiod7294323e5e542b7b37f48dd83565321
  solrVersion: solrVersion,
} else {}
