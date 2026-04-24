function Test-Url {
  param(
    [Parameter(Mandatory = $true)]
    [string] $Url,
    [hashtable] $Headers
  )

  try {
    $response = Invoke-WebRequest -Uri $Url -Headers $Headers -UseBasicParsing -ErrorAction Stop
    [pscustomobject]@{
      Url = $Url
      Ok = $true
      StatusCode = $response.StatusCode
      Message = 'OK'
    }
  }
  catch {
    [pscustomobject]@{
      Url = $Url
      Ok = $false
      StatusCode = $null
      Message = $_.Exception.Message
    }
  }
}

$backend = Test-Url -Url 'http://localhost:8080/actuator/health'
$frontend = Test-Url -Url 'http://localhost:3001'
$loginBody = @{ username = 'devuser'; password = 'devpass123' } | ConvertTo-Json

$login = try {
  Invoke-WebRequest -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body $loginBody -UseBasicParsing -ErrorAction Stop
} catch {
  $null
}

$authOk = $false
$authHeaders = $null
if ($login) {
  try {
    $loginJson = $login.Content | ConvertFrom-Json
    $authOk = [bool]$loginJson.token
    if ($authOk) {
      $authHeaders = @{ Authorization = "Bearer $($loginJson.token)" }
    }
  } catch {
    $authOk = $false
  }
}

$monitorSummary = [pscustomobject]@{ Ok = $false; StatusCode = $null; Message = 'AUTH_NOT_READY' }
$monitorRecent = [pscustomobject]@{ Ok = $false; StatusCode = $null; Message = 'AUTH_NOT_READY' }
if ($authHeaders) {
  $monitorSummary = Test-Url -Url 'http://localhost:8080/api/posts/monitor/summary' -Headers $authHeaders
  $monitorRecent = Test-Url -Url 'http://localhost:8080/api/posts/monitor/recent' -Headers $authHeaders
}

Write-Host ("BACKEND_OK={0} STATUS={1} MSG={2}" -f $backend.Ok, $backend.StatusCode, $backend.Message)
Write-Host ("FRONTEND_OK={0} STATUS={1} MSG={2}" -f $frontend.Ok, $frontend.StatusCode, $frontend.Message)
Write-Host ("AUTH_OK={0}" -f $authOk)
Write-Host ("MONITOR_SUMMARY_OK={0} STATUS={1} MSG={2}" -f $monitorSummary.Ok, $monitorSummary.StatusCode, $monitorSummary.Message)
Write-Host ("MONITOR_RECENT_OK={0} STATUS={1} MSG={2}" -f $monitorRecent.Ok, $monitorRecent.StatusCode, $monitorRecent.Message)

if ($backend.Ok -and $frontend.Ok -and $authOk -and $monitorSummary.Ok -and $monitorRecent.Ok) {
  Write-Host 'SMOKE_OK'
  exit 0
}

exit 1