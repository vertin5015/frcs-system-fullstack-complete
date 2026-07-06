# 检查本机爬虫端口是否在监听（与当前 application.yml 一致：9001 详情 + 9002/9003/9004 列表）
$ErrorActionPreference = 'SilentlyContinue'
$ports = @(9001, 9002, 9003, 9004)
Write-Host "Testing TCP connect to 127.0.0.1 ..."
foreach ($p in $ports) {
    $ok = (Test-NetConnection -ComputerName 127.0.0.1 -Port $p -WarningAction SilentlyContinue).TcpTestSucceeded
    Write-Host ("{0}`t{1}" -f $p, $ok)
}
Write-Host ""
Write-Host "False = 本机没有服务监听该端口，请先启动对应爬虫或修改 crawler.search / crawler.detail-url。"
