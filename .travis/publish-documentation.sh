#!/bin/bash

function prepare_ssh {
    openssl aes-256-cbc -K ${encrypted_1aab6c06035c_key} -iv ${encrypted_1aab6c06035c_iv} -in .travis/travis_rsa.enc -out .travis/travis_rsa -d
    chmod og-rwx .travis/travis_rsa
    eval `ssh-agent -s`
    ssh-add .travis/travis_rsa
}

function configure_git {
    git config --global user.email "travis@travis-ci.org"
    git config --global user.name "travis-ci"
}

function get_version {
    echo 'VERSION=${project.version}' | mvn help:evaluate | grep ^VERSION | sed -E 's/VERSION=(.*)/\1/'
}

function create_documentation {
    local VERSION="$1"
    mvn resources:testResources
    rm -rf docs/documentation/${VERSION} docs/documentation/*SNAPSHOT
    mkdir -p docs/documentation/${VERSION}
    mv target/test-classes/documentation/* docs/documentation/${VERSION}
}

function create_javadoc {
    local VERSION="$1"
    # The -D option is required here because the javadoc maven plugin does not work when specifying a different destination in report mode than in build mode.
    mvn clean javadoc:javadoc -DdestDir=${VERSION}
    rm -rf docs/javadoc/${VERSION} docs/javadoc/*SNAPSHOT
    mv target/site/apidocs/${VERSION} docs/javadoc/
}

function push_documentation {
    git add docs

    local CHANGED=$(git status -uno -s)

    if [ -n "$CHANGED" ]
    then
        git commit -m "Update documentation"
        git push
    else
        echo "No changes on documentation"
    fi
}

function publish_all_documentation {
    VERSION=$(get_version)
    create_documentation ${VERSION}
    create_javadoc ${VERSION}
    push_documentation
}

if [ ${TRAVIS} ] && [ "$TRAVIS_BRANCH" == "master" ] && [ -z "$TRAVIS_PULL_REQUEST_BRANCH" ]
then
    prepare_ssh
    configure_git
    git clone git@github.com:FaustXVI/junit5-docker.git out
    cd out
    publish_all_documentation
else
    echo "On a branch or not on travis, doing nothing !"
fi
