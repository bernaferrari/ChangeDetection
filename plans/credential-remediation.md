# Release credential containment

The tracked service-account JSON and signing keystore were removed from the working tree after byte-for-byte backup to `~/.config/changedetection/legacy-release-credentials/`. Directory permissions are 0700; files are 0600. Their source deletions are deliberately unstaged with the rest of this implementation.

No credential values are reproduced here. Removal from the working tree does not revoke a credential or remove it from existing Git history.

## External action still required

- Revoke/rotate the exposed Google service-account key through the owning Google Cloud project, checking the account's actual permissions and dependent release jobs first.
- Determine whether the keystore is the Play upload key or the app signing key. Use the appropriate Play Console upload-key reset/recovery process before replacing it. Do not generate a replacement and silently ship it: that can break upgrade compatibility.
- Once rotation is complete, coordinate removal from published Git history. Do not rewrite shared history or force-push without coordinating with the repository owner.

Repository code must load future release material through environment-configured paths outside the checkout. Never move the old files back into the repository to make a release pass.

Release builds now read signing properties only through `CHANGEDETECTION_SIGNING_PROPERTIES`; the repository's dummy properties are never used as signing input. Fastlane publishing requires that path and `CHANGEDETECTION_PLAY_CREDENTIALS`, both external files. Update the release version before explicitly running a publishing lane. No publishing lane has been run. Do not reuse the exposed credentials merely because a backup exists; rotate them first.
