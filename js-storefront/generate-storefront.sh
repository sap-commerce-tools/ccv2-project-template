#!/usr/bin/env sh

# yarn global add @angular/cli@v9-lts
ng new demoshop --style=scss --routing=false --packageManager=yarn
(
    cd demoshop || exit 1
    yarn ng add @spartacus/schematics --pwa --ssr
    yarn install
)
