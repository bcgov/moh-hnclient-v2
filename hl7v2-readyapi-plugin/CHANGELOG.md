# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Todo
- Add support for repeating Segments in the Assert HL7 Response assertion
- Remove Client/ClientCache/Connection classes as they are no longer used
- Use timeout settings when sending HL7 to HN Client
- Increase TextArea sizes in request/response windows

## [0.3.1] - 2021-09-20

### Changed
- Updated Encode HL7v2 Test Step to create Test Case properties from the HL7 request
- Updated Decoded HL7v2 to add the encodedHL7v2Msg as a Test Case property in addition to a context property
- Add the ability to Run the Encode HLyV2 Test Step independently

## [0.3.0] - 2021-09-17
### Added
- Added Encode HL7v2 Test Step which will encode the fileContent from the DataSource and make it available as a test case property
- Added Decode HL7v2 Assertion which will extract and decode the HL7v2 Response and make it available as a test case property
- Added Assert HL7v2 Response Assertion will will allow assertions to be performed on the HL7v2 Segment/Sequence/Component

### Changed
- Updated to ReadyAPI v.3.9.1 libraries

## [0.2.3] - 2021-06-01

### Added
- Added support for Data Source in message body
- Added support for sendingFacility token in request file/message body to be replaced by project setting at runtime

### Changed
- Tested in ReadyAPI v3.8.1

### Removed

### Fixed
- Endpoint in Test Panel UI now updates when Environment is changed

### Known Issues

## [0.2.2] - 2021-05-28

### Added
- HL7 Test Step now uses Environments/Endpoints instead of custom Connections

### Changed
- Added title to Test Step

### Removed

### Fixed

### Known Issues
- Endpoint in Test Panel UI doesn't update when Environment is changed

## [0.2.1] - 2021-05-12

### Added 

### Changed

### Removed

### Fixed
- Fixed rendering of icons

### Known Issues

## [0.2.0] - 2021-04-29

### Added 
- Add support for ReadyAPI v.3.7.0 libraries

### Changed
- Updated package structure

### Removed
- Removed unused TestSteps/Panels/Builders

### Fixed
- Fixed plugin naming in Integration->Enhancements
- Fixed terminology in Connection dialogs
- Fixed issue with Assertions not validating when value is added. Missing PropertyChangeListener

### Known Issues
- Valid/Failed icons don't rerender right away

## [0.1.0] - 2021-04-15

### Notes
- Got plug working in SoapUI 5.6.0

### Added
- Read connection parameters from Connection entries. No longer hardcoded
- Added EqualsAssertion which isn't available in SoapUI
- Added Valid and Failed test icons

### Changed
- Removed references to TyrusClient which was causing ClassLoader issues
- Updated publish step icon

### Removed
- Removed sample-websocket-server. Not relevant.
- Removed artwork. Not relevant.

### Known Issues
- IconAnimator doesn't work
- Valid/Failed icons don't rerender right away

## [0.0.1] - Legacy Version

### Notes
- Plugin no longer works in SoapUI 5.6.0

[unreleased]: https://github.com/olivierlacan/keep-a-changelog/compare/v1.1.0...HEAD
[0.0.1]: tag goes here