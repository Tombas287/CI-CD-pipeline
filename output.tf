output "public_ip" {
  value = module.linux_vm.linux_public_ip
}

output "azure_vm" {
  value = module.linux_vm.linux_username
}

output "aks_cluster" {
  value = module.aks_cluster.azurerm_aks
}