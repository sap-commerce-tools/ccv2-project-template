#!/usr/bin/env sh
set -e

RED=$(tput setaf 1)
GREEN=$(tput setaf 2)
YELLOW=$(tput setaf 3)
BRIGHT=$(tput bold)
NORMAL=$(tput sgr0)

progress() {
    printf "%b%b--- %s ---%b\n" "$GREEN" "$BRIGHT" "$1" "$NORMAL"
}
warning() {
    printf "%b%s%b\n" "$YELLOW" "$1" "$NORMAL"
}
error() {
    printf "%b%s%b\n" "$RED" "$1" "$NORMAL"
}

NAME=$1

if [ -z "$NAME" ]; then
    echo "Usage: ./bootstrap.sh <project name>"
    exit 1
fi

# https://sap.github.io/spartacus-docs/building-the-spartacus-storefront-from-libraries
# yarn global add @angular/cli@v10-lts

if ! command -v 'yarn' > /dev/null 2>&1
then
    error "yarn not found"
    error "please install it to continue"
    error "https://classic.yarnpkg.com/en/docs/install"
    exit 1
fi

if ! command -v 'ng' > /dev/null 2>&1
then
    error "Angular CLI (ng) not found"
    error "please install @angular/cli@v10-lts"
    error "yarn global add @angular/cli@v10-lts"
    exit 1
fi

NG_VERSION="$(ng version | grep '@angular-devkit/core' | awk '{ print $2 }')"
if case $NG_VERSION in 10*) false;; *) true;; esac; then
    error "Wrong angular version, please use Angular 10 (@angular/cli@v10-lts)"
    exit 1
fi

progress "Bootstrapping Angular project '$NAME'"
ng new "$NAME" --style=scss --routing=false --packageManager=yarn
(
    cd "$NAME" || exit 1
    progress "Adding Spartacus"
    ng add @spartacus/schematics --pwa --ssr
    # yarn install
    progress "Setting up SmartEdit support"
    INJECTOR="../../core-customize/hybris/bin/modules/smartedit/smarteditaddon/acceleratoraddon/web/webroot/_ui/shared/common/js/webApplicationInjector.js"
    if [ -f "$INJECTOR" ]; then
        cp "$INJECTOR" "src/assets"
    else
        warning "webApplicationInjector.js not found"
        warning "Please copy into the src/assets folder to enable SmartEdit"
    fi
    progress "Applying optimizations"
    cp -r "../bootstrap/.vscode" .
    for patch in ../bootstrap/*.patch; do
        patch -p0 < "$patch"
        success=$?
        if [ $success -ne 0 ]; then
            warning "could not apply patch $patch"
        fi
    done
)
progress "Generating Manifest"
if [ -f "manifest.json" ]; then
    backup="manifest.$(date +%F_%H%M%S).json"
    warning "manifest.json found; backing up to $backup"
    mv -f "manifest.json" "$backup"
fi
cat > manifest.json <<-EOF
{
    "applications": [{
        "name": "$NAME",
        "path": "$NAME",
        "csr": {
            "webroot": "dist/$NAME/browser/"
        },
        "ssr": {
            "enabled": true,
            "path": "dist/$NAME/server/main.js"
        }
    }]
}
EOF
progress "FINISHED"
echo "Next steps:"
echo "- Update the baseSite.context with the correct baseSite, currency etc."
echo "  https://sap.github.io/spartacus-docs/building-the-spartacus-storefront-from-libraries/#checking-appmodulets-for-base-url-and-other-settings"
echo "- (optional) Update smartedit whitelisting in src/index.html"
echo "  https://help.sap.com/viewer/9d346683b0084da2938be8a285c0c27a/LATEST/en-US/fb742b29cf3c4e81aac7c131c0441172.html"
