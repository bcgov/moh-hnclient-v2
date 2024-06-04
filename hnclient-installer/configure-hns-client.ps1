
$clientId = Read-Host -Prompt 'Enter Client ID '
$clientSecret = Read-Host -Prompt 'Enter Client Secret '

[Environment]::SetEnvironmentVariable("MOH_HNCLIENT_ID", $clientId, 'Machine')
[Environment]::SetEnvironmentVariable("MOH_HNCLIENT_SECRET", $clientSecret, 'Machine')

#To improve, add code to check if variables are set successfully and print the message accordingly.
Write-Host "If no errors, HNS-Client registeration complete and ready for Connections."
