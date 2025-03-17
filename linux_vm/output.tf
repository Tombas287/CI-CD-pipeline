output "linux_public_ip" {
  value = azurerm_public_ip.public_ip.ip_address
}

output "linux_username" {
  value = azurerm_linux_virtual_machine.vm.admin_username
}