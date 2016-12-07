#!/usr/bin/env bash

DOCS_FOLDER=docs
DOCUMENTATION_FOLDER=${DOCS_FOLDER}/documentation
JAVADOC_FOLDER=${DOCS_FOLDER}/javadoc

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
    rm -rf ${DOCUMENTATION_FOLDER}/${VERSION}
    if [[ ${VERSION} =~ "SNAPSHOT" ]]
    then
        rm -rf ${DOCUMENTATION_FOLDER}/*SNAPSHOT
    fi
    mkdir -p ${DOCUMENTATION_FOLDER}/${VERSION}
    mv target/test-classes/documentation/* ${DOCUMENTATION_FOLDER}/${VERSION}
}

function create_javadoc {
    local VERSION="$1"
    # The -D option is required here because the javadoc maven plugin does not work when specifying a different destination in report mode than in build mode.
    mvn clean javadoc:javadoc -DdestDir=${VERSION}
    rm -rf ${JAVADOC_FOLDER}/${VERSION}
    if [[ ${VERSION} =~ "SNAPSHOT" ]]
    then
        rm -rf ${JAVADOC_FOLDER}/*SNAPSHOT
    fi
    mv target/site/apidocs/${VERSION} ${JAVADOC_FOLDER}/
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

function update_link {
    cd $1
    ln -s -n -f $2 $3
    cd - > /dev/null
}

function update_symlinks {
    local VERSION="$1"
    if [[ ${VERSION} =~ "SNAPSHOT" ]]
    then
        update_link ${DOCUMENTATION_FOLDER} ${VERSION} snapshot
        update_link ${JAVADOC_FOLDER} ${VERSION} snapshot
    else
        update_link ${DOCUMENTATION_FOLDER} ${VERSION} current
        update_link ${JAVADOC_FOLDER} ${VERSION} current
    fi
}

function publish_all_documentation {
    local VERSION=$(get_version)
    create_documentation ${VERSION}
    create_javadoc ${VERSION}
    update_symlinks ${VERSION}
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
