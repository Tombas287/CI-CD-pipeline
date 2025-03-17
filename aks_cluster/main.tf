data "azurerm_resource_group" "rg" {
  name = var.resource_group
}

resource "azurerm_resource_group" "rg" {
  location = data.azurerm_resource_group.rg.location
  name     = data.azurerm_resource_group.rg.name
}

resource "azurerm_kubernetes_cluster" "aks" {
  location            = data.azurerm_resource_group.rg.location
  name                = "aks_cluster"
  resource_group_name = azurerm_resource_group.rg.name
  dns_prefix = "aksdns"

  default_node_pool {
    name    = var.node_pool
    vm_size = var.vm_size
    node_count = 2
    type = "VirtualMachineScaleSets"
  }
  identity {
    type = "SystemAssigned"
  }

  tags = {
    Environment = "Dev"
  }
}

