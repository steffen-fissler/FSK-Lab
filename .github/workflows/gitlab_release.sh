TARGET_FOLDER="$GITHUB_WORKSPACE/de.bund.bfr.knime.update/target"
REPO="repositories"

# Check Gitlab repo
git clone -b 4.5 --single-branch --depth=1 https://$GITLAB_NAME:$GITLAB_TOKEN@gitlab.bfr.berlin/silebat/$REPO.git

# Update build
rm -Rf $REPO/fsklab # Deletes old build if it exists
rm -Rf $REPO/knime_4.4 # Deletes old build if it exists

cd $REPO
date +"%Y-%m-%dT%H:%M:%S%z" >> version.info
git config --global user.email $GITLAB_EMAIL
git config --global user.name $GITLAB_TOKEN

git add .
git commit -m "Release"

# Push build
git push https://$GITLAB_NAME:$GITLAB_TOKEN@gitlab.bfr.berlin/silebat/$REPO.git 4.5