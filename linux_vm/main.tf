data "azurerm_resource_group" "rg" {
  name = var.azure_resource
}

resource "azurerm_resource_group" "rg" {
  location = data.azurerm_resource_group.rg.location
  name     = data.azurerm_resource_group.rg.location
}

resource "azurerm_virtual_network" "vnet" {
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.rg.location
  name                = "my_vnet"
  resource_group_name = azurerm_resource_group.rg.name
}

resource "azurerm_subnet" "subnet" {

  address_prefixes     = ["10.0.0.0/24"]
  name                 = "my_subnet"
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.vnet.name
}

resource "azurerm_public_ip" "public_ip" {
  allocation_method   = "Dynamic"
  location            = data.azurerm_resource_group.rg.location
  name                = "${var.azure_vm}-public-ip"
  resource_group_name = azurerm_resource_group.rg.name
  sku = "Basic"
}

resource "azurerm_network_interface" "nic" {
  location            = data.azurerm_resource_group.rg.location
  name                = "${var.azure_vm}-nic"
  resource_group_name = azurerm_resource_group.rg.name

  ip_configuration {
    name                          = "internal"
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id = azurerm_public_ip.public_ip.id
    subnet_id = azurerm_subnet.subnet.id
  }


}

resource "azurerm_network_security_group" "group" {
  location            = data.azurerm_resource_group.rg.location
  name                = "${var.azure_vm}-mygrp"
  resource_group_name = azurerm_resource_group.rg.name
}
resource "azurerm_network_security_rule" "allow_ssh" {
  name                        = "allow-ssh"
  priority                    = 100
  direction                   = "Inbound"
  access                      = "Allow"
  protocol                    = "Tcp"
  source_port_range           = "*"
  destination_port_range      = "22"
  source_address_prefix       = "*"
  destination_address_prefix  = "*"
  network_security_group_name = azurerm_network_security_group.group.name
  resource_group_name         = azurerm_resource_group.rg.name
}



resource "azurerm_network_interface_security_group_association" "association" {
  network_interface_id      = azurerm_network_interface.nic.id
  network_security_group_id = azurerm_network_security_group.group.id
}

resource "azurerm_availability_set" "availability" {
  location            = data.azurerm_resource_group.rg.location
  name                = "availability"
  resource_group_name = azurerm_resource_group.rg.name
  platform_fault_domain_count = 2  # Change from 3 to 2
  platform_update_domain_count = 5  # Optional, adjust as needed

  tags = {
    environment ="dev"

  }

}
resource "azurerm_linux_virtual_machine" "vm" {
  admin_username        = var.azure_vm
  location              = data.azurerm_resource_group.rg.location
  name                  = var.vm_name
  network_interface_ids = [azurerm_network_interface.nic.id]

  resource_group_name   = azurerm_resource_group.rg.name
  size                  = var.vm_size
  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"

  }

  source_image_reference {
    offer     = "0001-com-ubuntu-server-jammy"
    publisher = "Canonical"
    sku       = "22_04-lts-gen2"
    version   = "Latest"
  }


  admin_ssh_key {
    public_key = var.public_key
    username   = var.azure_vm
  }
  disable_password_authentication = true
  custom_data = base64encode(file("jenkins.sh"))

}


