# Play Store Submission Guide

## Current Status (2026-03-06)
Engineering gates are mostly green, but submission is blocked by unsigned bundle and missing local signing properties.

## Metadata Checklist
- App title, short description, full description
- Privacy policy URL
- Terms of service URL
- App category and contact details
- Screenshots + feature graphic
- Content rating questionnaire
- Data safety form

## Technical Checklist
1. Package ID confirmed: `com.aravya.apps.kosmos`
2. Signed AAB generated and verified
3. Target SDK policy alignment confirmed
4. Internal testing track rollout created

## Internal Track First Strategy
- Upload signed AAB to internal testing
- Add limited testers
- Run smoke test matrix on Play-distributed build
- Collect crash/ANR and critical functional defects

## Public Rollout Readiness
Do not move to production rollout until:
- zero P0/P1 blockers from internal track
- legal links are live and accurate
- support/contact channels are verified
- release notes and rollback plan are prepared

## Data Safety Inputs (prepare before console fill)
- Authentication data collected
- User profile and collaboration data flows
- Message/task/project content storage and transmission paths
- Data deletion/export behavior summary
