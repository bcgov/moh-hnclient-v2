# This script facilitates the configuration of the environment variables required for running an instance of the HNS Client. It allows these environment variables to be 
# set with unique names for each instance of the HNS Client based on the menu selection below. Variable pairs will be created with the following names for the listed instances:
#	Default
#		MOH_HNCLIENT_ID
#		MOH_HNCLIENT_SECRET
#	Test
#		MOH_HNCLIENT_ID_TEST
#		MOH_HNCLIENT_SECRET_TEST
#	Compliance
#		MOH_HNCLIENT_ID_VC1
#		MOH_HNCLIENT_SECRET_VC1
#	Training
#		MOH_HNCLIENT_TRN
#		MOH_HNCLIENT_SECRET_TRN
#	Production
#		MOH_HNCLIENT_PROD
#		MOH_HNCLIENT_SECRET_PROD

Write-Host "Begin configuring Environment Variables for the HNS Client instance..." 
$envVarSuffix=''
$clientId = Read-Host -Prompt 'Enter Client ID '
$clientSecret = Read-Host -Prompt 'Enter Client Secret '
$option = Read-Host -Prompt "Select installation type:`n
0)	Default installation (one host -> one environment)
1)	Multi-env host: Test
2)	Multi-env host: Compliance
3)	Multi-env host: Training
4)	Multi-env host: Production"

switch ($option)
{
    0 {
		break
	}
    1 {
		$envVarSuffix='_TEST';break
	}
    2 {
		$envVarSuffix='_VC1';break
	}
    3 {
		$envVarSuffix='_TRN';break
	}
    4 {
		$envVarSuffix='_PROD';break
	}
}

Write-Host "Setting Environment variables..." 
if ($envVarSuffix -ne '') {
	Write-Host "Using suffix: " $envVarSuffix 
}
[Environment]::SetEnvironmentVariable("MOH_HNCLIENT_ID" + $envVarSuffix, $clientId, 'Machine')
[Environment]::SetEnvironmentVariable("MOH_HNCLIENT_SECRET" + $envVarSuffix, $clientSecret, 'Machine')
Write-Host "Environment variables set."
Write-Host ("MOH_HNCLIENT_ID" + $envVarSuffix, ":", [Environment]::GetEnvironmentVariable('MOH_HNCLIENT_ID' + $envVarSuffix, 'Machine'))
Write-Host ("MOH_HNCLIENT_SECRET" + $envVarSuffix, ":", [Environment]::GetEnvironmentVariable('MOH_HNCLIENT_SECRET' + $envVarSuffix, 'Machine'))
Write-Host "Finished configuring this instance of HNS Client."
