;This file will be executed next to the application bundle image
;I.e. current directory will contain folder additional-bundler-files with application files
[Setup]
AppId={{de.dynamicfiles.projects.gradle.example}}
AppName=additional-bundler-files
AppVersion=1.0
AppVerName=additional-bundler-files 1.0
AppPublisher=YourName
AppComments=additional-bundler-files
AppCopyright=Copyright (C) 2016
;AppPublisherURL=http://java.com/
;AppSupportURL=http://java.com/
;AppUpdatesURL=http://java.com/
DefaultDirName={localappdata}\additional-bundler-files
DisableStartupPrompt=Yes
DisableDirPage=Yes
DisableProgramGroupPage=Yes
DisableReadyPage=Yes
DisableFinishedPage=Yes
DisableWelcomePage=Yes
DefaultGroupName=YourName
;Optional License

; modified version from the JavaFX-Gradle-Plugin project
; we modified the file by adding the filename without having
; this being part of the application resources
LicenseFile=license.rtf


;WinXP or above
MinVersion=0,5.1 
OutputBaseFilename=additional-bundler-files-1.0
Compression=lzma
SolidCompression=yes
PrivilegesRequired=lowest
SetupIconFile=additional-bundler-files\additional-bundler-files.ico
UninstallDisplayIcon={app}\additional-bundler-files.ico
UninstallDisplayName=additional-bundler-files
WizardImageStretch=No
WizardSmallImageFile=additional-bundler-files-setup-icon.bmp   
ArchitecturesInstallIn64BitMode=x64


[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "additional-bundler-files\additional-bundler-files.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "additional-bundler-files\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\additional-bundler-files"; Filename: "{app}\additional-bundler-files.exe"; IconFilename: "{app}\additional-bundler-files.ico"; Check: returnTrue()
Name: "{commondesktop}\additional-bundler-files"; Filename: "{app}\additional-bundler-files.exe";  IconFilename: "{app}\additional-bundler-files.ico"; Check: returnFalse()


[Run]
Filename: "{app}\additional-bundler-files.exe"; Parameters: "-Xappcds:generatecache"; Check: returnFalse()
Filename: "{app}\additional-bundler-files.exe"; Description: "{cm:LaunchProgram,additional-bundler-files}"; Flags: nowait postinstall skipifsilent; Check: returnTrue()
Filename: "{app}\additional-bundler-files.exe"; Parameters: "-install -svcName ""additional-bundler-files"" -svcDesc ""additional-bundler-files"" -mainExe ""additional-bundler-files.exe""  "; Check: returnFalse()

[UninstallRun]
Filename: "{app}\additional-bundler-files.exe "; Parameters: "-uninstall -svcName additional-bundler-files -stopOnUninstall"; Check: returnFalse()

[Code]
function returnTrue(): Boolean;
begin
  Result := True;
end;

function returnFalse(): Boolean;
begin
  Result := False;
end;

function InitializeSetup(): Boolean;
begin
// Possible future improvements:
//   if version less or same => just launch app
//   if upgrade => check if same app is running and wait for it to exit
//   Add pack200/unpack200 support? 
  Result := True;
end;  
