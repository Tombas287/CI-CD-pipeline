
module "linux_vm" {
  source = "./linux_vm"
  azure_location = var.azure_location
  azure_resource = var.azure_resource
  vm_name = "linux-vm"
  vm_size = "Standard_D2s_v3"
  azure_vm = "adminuser"
  public_key = file("~/.ssh/id_rsa.pub")
}

#module "storage_account" {
#  source = "./storage_account"
#  azure_resource = var.azure_resource
#  azure_location = var.azure_location
#  storage_account_name ="mystorage543232"
#}

module "aks_cluster" {
  source = "./aks_cluster"
  location = var.azure_location
  resource_group = var.azure_resource
  node_pool = "default"
  vm_size = "Standard_DS2_v2"

}