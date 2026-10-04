# Translation review fixes

Owner request, 2026-10-04: apply the review one item per commit on `chs-translation`; do not push.
Use the existing AppCompat locale APIs and resource strings; add no dependencies.
Run focused checks per commit, then `scripts/ladder.sh` once after all commits (owner, 2026-10-04).
Runtime changes also require the RUNBOOK glue-review pass. Device appearance remains owner-verified.

- [x] Review 4: set vc27 / 1.13.0 in `app/build.gradle.kts`; add changelog 27, preserve 26.
- [x] Review 1: resolve language once in each notification build and pass the context to actions.
- [x] Review 2: add translated System default to `OnboardingScreen.kt`; clear application locales.
- [x] Review 3: give the six profile-list labels dedicated English/Chinese `settings_*` resources.
- [x] Review 5: update existing notification channel names when the service configuration changes;
  test both channels and preserve their settings.
- [ ] Review 6: inspect the assembled launch theme; retain AppCompat unless a concrete defect is found.
- [ ] Nit: move the language-picker reference into RUNBOOK's reference index.
- [ ] Nit: compress the six completed translation changelog entries in STATE into one.

For notification changes, reuse `AmbientMonitoringServiceTest`; for System default, exercise the
picker through `UiShellTest`; for list labels, check localized display rows in `SettingsDisplayTest`.
Use focused failing tests for behavior fixes, then the full ladder. Each checkpoint updates STATE
and this checklist. Delete this plan when complete; retain the outcome in STATE's changelog.
Leave `help_pwm_exponent` and CONTRIBUTING policy updates to the maintainer's separate branch.
