module com.udacity.catpoint.security {
    requires java.desktop;
    requires com.udacity.catpoint.image;
    requires com.google.common;
    requires java.prefs;
    requires miglayout;
    requires com.google.gson;
    opens com.udacity.catpoint.security.data to com.google.gson;
}