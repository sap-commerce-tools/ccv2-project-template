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

# https://sap.github.io/spartacus-docs/building-the-spartacus-storefront-from-libraries-4-x/
# yarn global add @angular/cli@latest

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
    error "please install @angular/cli@17"
    error "> yarn global add @angular/cli@17"
    exit 1
fi

NG_VERSION="$(ng version | grep '@angular-devkit/core' | awk '{ print $2 }')"
if case $NG_VERSION in 17.*) false;; *) true;; esac; then
    error "Wrong angular version, please use Angular 17 (@angular/cli@latest)"
    exit 1
fi

echo "> Please visit https://ui.repositories.cloud.sap/www/webapp/users and copy your base64 credentials."
echo "> (create a user if needed)"
read -p "Enter base64: " npmcredentials

progress "Bootstrapping Angular project '$NAME'"
ng new "$NAME" \
  --skip-install \
  --skip-git \
  --style=scss \
  --standalone=false \
  --routing=false \
  --package-manager=yarn
(
    cd "$NAME" || exit 1

cat > .npmrc <<-EOF
@spartacus:registry=https://73554900100900004337.npmsrv.base.repositories.cloud.sap/
//73554900100900004337.npmsrv.base.repositories.cloud.sap/:_auth=$npmcredentials
always-auth=true
EOF

    progress "Adding Spartacus (PWA and SSR enabled)"
    echo "> Recommended minimal features: Cart, Product, SmartEdit"
    echo "> Just confirm the empty defaults for SmartEdit preview route and allow origin"
    ng add @spartacus/schematics@2211.23 \
      --pwa \
      --ssr \
      --use-meta-tags
    progress "Applying optimizations"
    cp -r "../bootstrap/.vscode" .
    angular="$(grep -i '@angular/animations' package.json | awk '{ print $2 }')"
    mkdir -p "patches"
    for template in ../bootstrap/patches/*.patch; do
        output="patches/$(basename "$template")"
        sed "s/@NAME@/$NAME/g" "$template" > "$output.1"
        sed "s/@ANGULAR@/$angular/g" "$output.1" > "$output"
        rm "$output.1"
    done
    for patch in patches/*.patch; do
        patch -p0 < "$patch" || true
    done
    yarn install
)
progress "Generating Manifest"
if [ -f "manifest.json" ]; then
    backup="manifest.$(date +%F_%H%M%S).json"
    warning "manifest.json found; backing up to $backup"
    mv -f "manifest.json" "$backup"
fi
cat > manifest.json <<-EOF
{
    "applications": [
        {
            "name": "$NAME",
            "path": "$NAME",
            "ssr": {
                "enabled": true,
                "path": "dist/$NAME/server/main.js"
            },
            "csr": {
                "webroot": "dist/$NAME/browser/"
            }
        }
    ],
    "nodeVersion": "20"
}
EOF
progress "FINISHED"
echo "Next steps:"
echo "- Update the baseSite.context with the correct baseSite, currency etc."
echo "  https://sap.github.io/spartacus-docs/building-the-spartacus-storefront-from-libraries-4-x/#checking-spartacus-configurationmodulets-for-base-url-and-other-settings"
echo "- Update smartedit whitelisting in spartacus-configuration.module.ts"
echo "  https://sap.github.io/spartacus-docs/smartEdit-setup-instructions-for-spartacus/"
