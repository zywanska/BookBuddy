package com.example.bookbuddy.homeView

class ModelPost(
    var description: String? = null,
    var ptime: String? = null,
    var title: String? = null,
    var uid: String? = null,
    var uname: String? = null,
    var upic: String? = null,
    var uemail: String? = null,
    var city: String? = null
) {
    constructor() : this("", "", "", "", "", "", "", "")
}

