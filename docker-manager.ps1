# Docker Compose Manager - Event Ledger Services
# PowerShell Script for Windows users
# Usage: .\docker-manager.ps1 [command]

param(
    [string]$Command = "help"
)

# Colors for output
$Colors = @{
    Success = "Green"
    Error   = "Red"
    Warning = "Yellow"
    Info    = "Cyan"
}

function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

function Show-Menu {
    Write-ColorOutput "`n╔════════════════════════════════════════════════════════════╗" $Colors.Info
    Write-ColorOutput "║    Docker Compose Manager - Event Ledger Services           ║" $Colors.Info
    Write-ColorOutput "╚════════════════════════════════════════════════════════════╝`n" $Colors.Info
    
    Write-Host "Available Commands:" -ForegroundColor Cyan
    Write-Host "`n  start         - Start all services"
    Write-Host "  stop          - Stop all services"
    Write-Host "  restart       - Restart all services"
    Write-Host "  rebuild       - Rebuild and start services"
    Write-Host "  logs          - Show live logs"
    Write-Host "  logs:gateway  - Show gateway service logs"
    Write-Host "  logs:account  - Show account service logs"
    Write-Host "  status        - Show service status"
    Write-Host "  clean         - Stop and remove containers"
    Write-Host "  reset         - Complete reset (remove volumes and images)"
    Write-Host "  help          - Show this menu`n"
}

function Test-DockerInstalled {
    try {
        $dockerVersion = docker --version 2>$null
        $composeVersion = docker-compose --version 2>$null
        
        if ($dockerVersion -and $composeVersion) {
            Write-ColorOutput "✓ Docker and Docker Compose are installed" $Colors.Success
            Write-Host "  $dockerVersion"
            Write-Host "  $composeVersion`n"
            return $true
        } else {
            Write-ColorOutput "✗ Docker or Docker Compose not found" $Colors.Error
            return $false
        }
    } catch {
        Write-ColorOutput "✗ Error checking Docker installation: $_" $Colors.Error
        return $false
    }
}

function Start-Services {
    Write-ColorOutput "`n🚀 Starting services..." $Colors.Info
    docker-compose up -d
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "✓ Services started successfully" $Colors.Success
        Show-Status
    } else {
        Write-ColorOutput "✗ Failed to start services" $Colors.Error
    }
}

function Stop-Services {
    Write-ColorOutput "`n⏹️  Stopping services..." $Colors.Warning
    docker-compose stop
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "✓ Services stopped successfully" $Colors.Success
    } else {
        Write-ColorOutput "✗ Failed to stop services" $Colors.Error
    }
}

function Restart-Services {
    Write-ColorOutput "`n🔄 Restarting services..." $Colors.Info
    docker-compose restart
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "✓ Services restarted successfully" $Colors.Success
        Show-Status
    } else {
        Write-ColorOutput "✗ Failed to restart services" $Colors.Error
    }
}

function Rebuild-Services {
    Write-ColorOutput "`n🔨 Rebuilding and starting services..." $Colors.Info
    docker-compose up -d --build
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "✓ Services rebuilt and started successfully" $Colors.Success
        Show-Status
    } else {
        Write-ColorOutput "✗ Failed to rebuild services" $Colors.Error
    }
}

function Show-Logs {
    Write-ColorOutput "`n📋 Showing live logs (Ctrl+C to exit)..." $Colors.Info
    docker-compose logs -f
}

function Show-GatewayLogs {
    Write-ColorOutput "`n📋 Showing gateway service logs (Ctrl+C to exit)..." $Colors.Info
    docker-compose logs -f gateway-service
}

function Show-AccountLogs {
    Write-ColorOutput "`n📋 Showing account service logs (Ctrl+C to exit)..." $Colors.Info
    docker-compose logs -f account-service
}

function Show-Status {
    Write-ColorOutput "`n📊 Service Status:" $Colors.Info
    Write-Host "─────────────────────────────────────────────────────────────`n"
    docker-compose ps
    Write-Host "`n─────────────────────────────────────────────────────────────"
    
    # Check if services are healthy
    Write-Host "`n🔍 Endpoint Access:"
    Write-Host "  Gateway API:      http://localhost:8080"
    Write-Host "  Swagger UI:       http://localhost:8080/swagger-ui.html"
    Write-Host "  Health Check:     http://localhost:8080/actuator/health"
    Write-Host "  Account Service:  http://account-service:8080 (internal only)`n"
}

function Clean-Services {
    Write-ColorOutput "`n🧹 Cleaning up (removing containers)..." $Colors.Warning
    docker-compose down
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "✓ Cleanup completed" $Colors.Success
    } else {
        Write-ColorOutput "✗ Cleanup failed" $Colors.Error
    }
}

function Reset-Complete {
    Write-ColorOutput "`n⚠️  WARNING: This will remove all containers, volumes, and images!" $Colors.Warning
    $confirm = Read-Host "Are you sure? (yes/no)"
    
    if ($confirm -eq "yes") {
        Write-ColorOutput "`n🔄 Performing complete reset..." $Colors.Warning
        
        docker-compose down -v
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "✓ Containers and volumes removed" $Colors.Success
        }
        
        Write-Host "Removing images..."
        docker image rm account-service gateway-service 2>$null
        
        Write-ColorOutput "`n✓ Complete reset finished" $Colors.Success
        Write-Host "Run 'docker-compose up -d --build' to start fresh`n"
    } else {
        Write-ColorOutput "✓ Reset cancelled" $Colors.Info
    }
}

# Main execution
Write-Host ""

# Check Docker installation
if (-not (Test-DockerInstalled)) {
    Write-ColorOutput "Please install Docker and Docker Compose" $Colors.Error
    exit 1
}

# Check if docker-compose.yml exists
if (-not (Test-Path "docker-compose.yml")) {
    Write-ColorOutput "Error: docker-compose.yml not found in current directory" $Colors.Error
    Write-Host "Please run this script from the workspace root directory`n"
    exit 1
}

# Execute command
switch ($Command.ToLower()) {
    "start" { Start-Services }
    "stop" { Stop-Services }
    "restart" { Restart-Services }
    "rebuild" { Rebuild-Services }
    "logs" { Show-Logs }
    "logs:gateway" { Show-GatewayLogs }
    "logs:account" { Show-AccountLogs }
    "status" { Show-Status }
    "clean" { Clean-Services }
    "reset" { Reset-Complete }
    "help" { Show-Menu }
    default {
        Write-ColorOutput "Unknown command: $Command`n" $Colors.Error
        Show-Menu
        exit 1
    }
}

Write-Host ""
