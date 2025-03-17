terraform {
  backend "azurerm" {
    storage_account_name = "terraformst123"
    container_name = "terraformstatecontainer"
    key = "terraform.tfstate"
    access_key = ""

  }

}


provider "azurerm" {
  features {}
  subscription_id = ""

}
