package com.commander4j.sys;

import java.awt.Color;
import java.awt.Font;
import java.io.File;

import javax.swing.ImageIcon;

public class Common
{
    public static String iconPath = "." + File.separator + "images" + File.separator + "appIcons" + File.separator;

    public static final String programName = "Commander4j Modbus Client";
    public static final String version     = "1.23";
    public static final String helpURL     = "https://wiki.commander4j.com/index.php?title=ModbusClient";

    public static String buildTitle(String filename)
    {
        String base = programName + " " + version;
        if (filename == null || filename.isEmpty()) return base;
        return base + " [" + filename + "]";
    }

    public static int LFAdjustWidth           = 0;
    public static int LFAdjustHeight          = 0;
    public static int LFTreeMenuAdjustWidth   = 0;
    public static int LFTreeMenuAdjustHeight  = 0;

    public final static ImageIcon icon_about         = new ImageIcon(iconPath + "about_24x24.png");
    public final static ImageIcon icon_help          = new ImageIcon(iconPath + "help_24x24.png");
    public final static ImageIcon icon_license       = new ImageIcon(iconPath + "open_source_24x24.png");
    public final static ImageIcon icon_ok            = new ImageIcon(iconPath + "ok_24x24.png");
    public final static ImageIcon icon_cancel        = new ImageIcon(iconPath + "cancel_24x24.png");
    public final static ImageIcon icon_exit          = new ImageIcon(iconPath + "exit_24x24.png");
    public final static ImageIcon icon_erase         = new ImageIcon(iconPath + "erase_24x24.png");
    public final static ImageIcon icon_eraser        = new ImageIcon(iconPath + "eraser_24x24.png");
    public final static ImageIcon icon_save          = new ImageIcon(iconPath + "save_24x24.png");
    public final static ImageIcon icon_open          = new ImageIcon(iconPath + "open_file_24x24.png");
    public final static ImageIcon icon_start         = new ImageIcon(iconPath + "bolt_24x24.png");
    public final static ImageIcon icon_stop          = new ImageIcon(iconPath + "ban_24x24.png");
    public final static ImageIcon icon_connected     = new ImageIcon(iconPath + "connected.png");
    public final static ImageIcon icon_disconnected  = new ImageIcon(iconPath + "disconnected.png");

    public final static Font font_std        = new Font("Arial", Font.PLAIN, 11);
    public final static Font font_bold       = new Font("Arial", Font.BOLD, 11);
    public final static Font font_btn        = new Font("Arial", Font.PLAIN, 11);
    public final static Font font_input      = new Font("Arial", Font.PLAIN, 11);
    public final static Font font_combo      = new Font("Monospaced", Font.PLAIN, 11);
    public final static Font font_popup      = new Font("Arial", Font.PLAIN, 11);
    public final static Font font_list       = new Font("Monospaced", 0, 11);
    public final static Font font_menu       = new Font("Arial", Font.PLAIN, 12);
    public final static Font font_btn_bold   = new Font("Arial", Font.BOLD, 9);
    public final static Font font_btn_small  = new Font("Arial", Font.PLAIN, 9);

    public final static Color color_button                              = new Color(233, 236, 242);
    public final static Color color_button_hover                        = new Color(160, 160, 160);
    public final static Color color_button_font                         = Color.BLACK;
    public final static Color color_button_font_hover                   = Color.BLACK;
    public final static Color color_textfield_background_focus_color    = new Color(255, 255, 200);
    public final static Color color_textfield_background_nofocus_color  = Color.WHITE;
    public final static Color color_textfield_foreground_focus_color    = Color.BLACK;
    public final static Color color_textfield_forground_nofocus_color   = Color.BLACK;
    public final static Color color_textfield_foreground_disabled       = Color.BLUE;
    public final static Color color_textfield_background_disabled       = new Color(241, 241, 241);
    public final static Color color_text_maxsize_color                  = Color.RED;
    public final static Color color_app_window                          = new Color(241, 241, 241);
    public final static Color color_listBackground                      = new Color(243, 251, 255);
    public final static Color color_listSelectionBackground             = new Color(51, 122, 183);
    public final static Color color_listSelectionForeground             = Color.WHITE;
}
