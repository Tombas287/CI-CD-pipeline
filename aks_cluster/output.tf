output "azurerm_aks" {
  value = azurerm_kubernetes_cluster.aks.fqdn
}