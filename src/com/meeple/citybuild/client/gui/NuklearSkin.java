package com.meeple.citybuild.client.gui;

import static org.lwjgl.nuklear.Nuklear.*;

import java.util.function.Consumer;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkStyleScrollbar;
import org.lwjgl.nuklear.NkStyleWindow;
import org.lwjgl.nuklear.NkStyleWindowHeader;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.nuklear.NuklearManager;

public class NuklearSkin {

	public void loadSkin(NkContextSingleton context, MemoryStack stack) {
		/*
				NkContext ctx = context.context;
				
				int skin = image_load("../skins/gwen.png");
				NkRect rect = NkRect.callocStack(stack);
				rect.set(128,23,127,104);
		//		NkImage window = nk_subimage_id(skin, 512,512, rect);
		media.skin = image_load("../skins/gwen.png");
		media.check = nk_subimage_id(media.skin, 512,512, nk_rect(464,32,15,15));
		media.check_cursor = nk_subimage_id(media.skin, 512,512, nk_rect(450,34,11,11));
		media.option = nk_subimage_id(media.skin, 512,512, nk_rect(464,64,15,15));
		media.option_cursor = nk_subimage_id(media.skin, 512,512, nk_rect(451,67,9,9));
		media.header = nk_subimage_id(media.skin, 512,512, nk_rect(128,0,127,24));
		media.window = nk_subimage_id(media.skin, 512,512, nk_rect(128,23,127,104));
		media.scrollbar_inc_button = nk_subimage_id(media.skin, 512,512, nk_rect(464,256,15,15));
		media.scrollbar_inc_button_hover = nk_subimage_id(media.skin, 512,512, nk_rect(464,320,15,15));
		media.scrollbar_dec_button = nk_subimage_id(media.skin, 512,512, nk_rect(464,224,15,15));
		media.scrollbar_dec_button_hover = nk_subimage_id(media.skin, 512,512, nk_rect(464,288,15,15));
		media.button = nk_subimage_id(media.skin, 512,512, nk_rect(384,336,127,31));
		media.button_hover = nk_subimage_id(media.skin, 512,512, nk_rect(384,368,127,31));
		media.button_active = nk_subimage_id(media.skin, 512,512, nk_rect(384,400,127,31));
		media.tab_minimize = nk_subimage_id(media.skin, 512,512, nk_rect(451, 99, 9, 9));
		media.tab_maximize = nk_subimage_id(media.skin, 512,512, nk_rect(467,99,9,9));
		media.slider = nk_subimage_id(media.skin, 512,512, nk_rect(418,33,11,14));
		media.slider_hover = nk_subimage_id(media.skin, 512,512, nk_rect(418,49,11,14));
		media.slider_active = nk_subimage_id(media.skin, 512,512, nk_rect(418,64,11,14));
		
		 window 
		ctx.style().window().background().r((byte)204).g((byte)204).b((byte)204);
		//        ctx.style().window().fixed_background = nk_style_item_image(media.window);
		ctx.style().window(new Consumer<NkStyleWindow>() {
					
					@Override
					public void accept(NkStyleWindow t) {
		
				        NuklearManager.setNkColour(t.background(),204,204,204);
		//		        t.fixed_background().set(data)
				        NuklearManager.setNkColour(t.border_color(),67,67,67);
				        
				        
				        
						NuklearManager.setNkColour(t.combo_border_color(),67,67,67);
						NuklearManager.setNkColour(t.contextual_border_color(), 67, 67, 67);
						NuklearManager.setNkColour(t.menu_border_color(), 67, 67, 67);
						NuklearManager.setNkColour(t.group_border_color(), 67, 67, 67);
						NuklearManager.setNkColour(t.tooltip_border_color(), 67, 67, 67);
						t.scrollbar_size().set(16,16);
						NuklearManager.setNkColour(t.border_color(), 0,0,0,0);
						t.padding().set(8,4);
						t.border(3f);
						
					}
				});
		ctx.style().window().header(new Consumer<NkStyleWindowHeader>() {
		
					@Override
					public void accept(NkStyleWindowHeader t) {
						nk_style_item_image(header, t.normal());
						nk_style_item_image(header,t.hover());
						nk_style_item_image(header,t.active());
						NuklearManager.setNkColour(t.label_normal(),95,95,95);
						NuklearManager.setNkColour(t.label_hover(),95,95,95);
						NuklearManager.setNkColour(t.label_active(),95,95,95);
					}
				});
		
		 scrollbar 
		ctx.style().scrollv(new Consumer<NkStyleScrollbar>() {
		
					@Override
					public void accept(NkStyleScrollbar t) {
						NkColor col = NkColor.callocStack(stack);
						nk_style_item_color(nk_rgb(184,184,184,col),t.normal());
						nk_style_item_color(nk_rgb(184,184,184,col),t.hover());
						nk_style_item_color(nk_rgb(184,184,184,col),t.active());
						nk_style_item_color(nk_rgb(184,184,184,col),t.cursor_normal());
						nk_style_item_color(nk_rgb(184,184,184,col),t.cursor_hover());
						nk_style_item_color(nk_rgb(184,184,184,col),t.cursor_active());
						t.dec_symbol(NK_SYMBOL_NONE);
						t.inc_symbol(NK_SYMBOL_NONE);
						t.show_buttons(nk_true);
						nk_style_item_color(nk_rgb(81,81,81,col),t.border_color());
						nk_style_item_color(nk_rgb(81,81,81,col),t.cursor_border_color());
						t.border(1f);
						t.rounding(0f);
						t.border_cursor(1f);
						t.rounding_cursor(2f);
					}
					
			
				});
		
		 scrollbar buttons 
		ctx.style().scrollv.inc_button.normal          = nk_style_item_image(media.scrollbar_inc_button);
		ctx.style().scrollv.inc_button.hover           = nk_style_item_image(media.scrollbar_inc_button_hover);
		ctx.style().scrollv.inc_button.active          = nk_style_item_image(media.scrollbar_inc_button_hover);
		ctx.style().scrollv.inc_button.border_color    = nk_rgba(0,0,0,0);
		ctx.style().scrollv.inc_button.text_background = nk_rgba(0,0,0,0);
		ctx.style().scrollv.inc_button.text_normal     = nk_rgba(0,0,0,0);
		ctx.style().scrollv.inc_button.text_hover      = nk_rgba(0,0,0,0);
		ctx.style().scrollv.inc_button.text_active     = nk_rgba(0,0,0,0);
		ctx.style().scrollv.inc_button.border          = 0.0f;
		
		ctx.style().scrollv.dec_button.normal          = nk_style_item_image(media.scrollbar_dec_button);
		ctx.style().scrollv.dec_button.hover           = nk_style_item_image(media.scrollbar_dec_button_hover);
		ctx.style().scrollv.dec_button.active          = nk_style_item_image(media.scrollbar_dec_button_hover);
		ctx.style().scrollv.dec_button.border_color    = nk_rgba(0,0,0,0);
		ctx.style().scrollv.dec_button.text_background = nk_rgba(0,0,0,0);
		ctx.style().scrollv.dec_button.text_normal     = nk_rgba(0,0,0,0);
		ctx.style().scrollv.dec_button.text_hover      = nk_rgba(0,0,0,0);
		ctx.style().scrollv.dec_button.text_active     = nk_rgba(0,0,0,0);
		ctx.style().scrollv.dec_button.border          = 0.0f;
		
		 checkbox toggle 
		{struct nk_style_toggle *toggle;
		toggle = &ctx.style().checkbox;
		toggle->normal          = nk_style_item_image(media.check);
		toggle->hover           = nk_style_item_image(media.check);
		toggle->active          = nk_style_item_image(media.check);
		toggle->cursor_normal   = nk_style_item_image(media.check_cursor);
		toggle->cursor_hover    = nk_style_item_image(media.check_cursor);
		toggle->text_normal     = nk_rgb(95,95,95);
		toggle->text_hover      = nk_rgb(95,95,95);
		toggle->text_active     = nk_rgb(95,95,95);}
		
		 option toggle 
		{struct nk_style_toggle *toggle;
		toggle = &ctx.style().option;
		toggle->normal          = nk_style_item_image(media.option);
		toggle->hover           = nk_style_item_image(media.option);
		toggle->active          = nk_style_item_image(media.option);
		toggle->cursor_normal   = nk_style_item_image(media.option_cursor);
		toggle->cursor_hover    = nk_style_item_image(media.option_cursor);
		toggle->text_normal     = nk_rgb(95,95,95);
		toggle->text_hover      = nk_rgb(95,95,95);
		toggle->text_active     = nk_rgb(95,95,95);}
		
		 default button 
		ctx.style().button.normal = nk_style_item_image(media.button);
		ctx.style().button.hover = nk_style_item_image(media.button_hover);
		ctx.style().button.active = nk_style_item_image(media.button_active);
		ctx.style().button.border_color = nk_rgba(0,0,0,0);
		ctx.style().button.text_background = nk_rgba(0,0,0,0);
		ctx.style().button.text_normal = nk_rgb(95,95,95);
		ctx.style().button.text_hover = nk_rgb(95,95,95);
		ctx.style().button.text_active = nk_rgb(95,95,95);
		
		 default text 
		ctx.style().text.color = nk_rgb(95,95,95);
		
		 contextual button 
		ctx.style().contextual_button.normal = nk_style_item_color(nk_rgb(206,206,206));
		ctx.style().contextual_button.hover = nk_style_item_color(nk_rgb(229,229,229));
		ctx.style().contextual_button.active = nk_style_item_color(nk_rgb(99,202,255));
		ctx.style().contextual_button.border_color = nk_rgba(0,0,0,0);
		ctx.style().contextual_button.text_background = nk_rgba(0,0,0,0);
		ctx.style().contextual_button.text_normal = nk_rgb(95,95,95);
		ctx.style().contextual_button.text_hover = nk_rgb(95,95,95);
		ctx.style().contextual_button.text_active = nk_rgb(95,95,95);
		
		 menu button 
		ctx.style().menu_button.normal = nk_style_item_color(nk_rgb(206,206,206));
		ctx.style().menu_button.hover = nk_style_item_color(nk_rgb(229,229,229));
		ctx.style().menu_button.active = nk_style_item_color(nk_rgb(99,202,255));
		ctx.style().menu_button.border_color = nk_rgba(0,0,0,0);
		ctx.style().menu_button.text_background = nk_rgba(0,0,0,0);
		ctx.style().menu_button.text_normal = nk_rgb(95,95,95);
		ctx.style().menu_button.text_hover = nk_rgb(95,95,95);
		ctx.style().menu_button.text_active = nk_rgb(95,95,95);
		
		 tree 
		ctx.style().tab.text = nk_rgb(95,95,95);
		ctx.style().tab.tab_minimize_button.normal = nk_style_item_image(media.tab_minimize);
		ctx.style().tab.tab_minimize_button.hover = nk_style_item_image(media.tab_minimize);
		ctx.style().tab.tab_minimize_button.active = nk_style_item_image(media.tab_minimize);
		ctx.style().tab.tab_minimize_button.text_background = nk_rgba(0,0,0,0);
		ctx.style().tab.tab_minimize_button.text_normal = nk_rgba(0,0,0,0);
		ctx.style().tab.tab_minimize_button.text_hover = nk_rgba(0,0,0,0);
		ctx.style().tab.tab_minimize_button.text_active = nk_rgba(0,0,0,0);
		
		ctx.style().tab.tab_maximize_button.normal = nk_style_item_image(media.tab_maximize);
		ctx.style().tab.tab_maximize_button.hover = nk_style_item_image(media.tab_maximize);
		ctx.style().tab.tab_maximize_button.active = nk_style_item_image(media.tab_maximize);
		ctx.style().tab.tab_maximize_button.text_background = nk_rgba(0,0,0,0);
		ctx.style().tab.tab_maximize_button.text_normal = nk_rgba(0,0,0,0);
		ctx.style().tab.tab_maximize_button.text_hover = nk_rgba(0,0,0,0);
		ctx.style().tab.tab_maximize_button.text_active = nk_rgba(0,0,0,0);
		
		ctx.style().tab.node_minimize_button.normal = nk_style_item_image(media.tab_minimize);
		ctx.style().tab.node_minimize_button.hover = nk_style_item_image(media.tab_minimize);
		ctx.style().tab.node_minimize_button.active = nk_style_item_image(media.tab_minimize);
		ctx.style().tab.node_minimize_button.text_background = nk_rgba(0,0,0,0);
		ctx.style().tab.node_minimize_button.text_normal = nk_rgba(0,0,0,0);
		ctx.style().tab.node_minimize_button.text_hover = nk_rgba(0,0,0,0);
		ctx.style().tab.node_minimize_button.text_active = nk_rgba(0,0,0,0);
		
		ctx.style().tab.node_maximize_button.normal = nk_style_item_image(media.tab_maximize);
		ctx.style().tab.node_maximize_button.hover = nk_style_item_image(media.tab_maximize);
		ctx.style().tab.node_maximize_button.active = nk_style_item_image(media.tab_maximize);
		ctx.style().tab.node_maximize_button.text_background = nk_rgba(0,0,0,0);
		ctx.style().tab.node_maximize_button.text_normal = nk_rgba(0,0,0,0);
		ctx.style().tab.node_maximize_button.text_hover = nk_rgba(0,0,0,0);
		ctx.style().tab.node_maximize_button.text_active = nk_rgba(0,0,0,0);
		
		 selectable 
		ctx.style().selectable.normal = nk_style_item_color(nk_rgb(206,206,206));
		ctx.style().selectable.hover = nk_style_item_color(nk_rgb(206,206,206));
		ctx.style().selectable.pressed = nk_style_item_color(nk_rgb(206,206,206));
		ctx.style().selectable.normal_active = nk_style_item_color(nk_rgb(185,205,248));
		ctx.style().selectable.hover_active = nk_style_item_color(nk_rgb(185,205,248));
		ctx.style().selectable.pressed_active = nk_style_item_color(nk_rgb(185,205,248));
		ctx.style().selectable.text_normal = nk_rgb(95,95,95);
		ctx.style().selectable.text_hover = nk_rgb(95,95,95);
		ctx.style().selectable.text_pressed = nk_rgb(95,95,95);
		ctx.style().selectable.text_normal_active = nk_rgb(95,95,95);
		ctx.style().selectable.text_hover_active = nk_rgb(95,95,95);
		ctx.style().selectable.text_pressed_active = nk_rgb(95,95,95);
		
		 slider 
		ctx.style().slider.normal          = nk_style_item_hide();
		ctx.style().slider.hover           = nk_style_item_hide();
		ctx.style().slider.active          = nk_style_item_hide();
		ctx.style().slider.bar_normal      = nk_rgb(156,156,156);
		ctx.style().slider.bar_hover       = nk_rgb(156,156,156);
		ctx.style().slider.bar_active      = nk_rgb(156,156,156);
		ctx.style().slider.bar_filled      = nk_rgb(156,156,156);
		ctx.style().slider.cursor_normal   = nk_style_item_image(media.slider);
		ctx.style().slider.cursor_hover    = nk_style_item_image(media.slider_hover);
		ctx.style().slider.cursor_active   = nk_style_item_image(media.slider_active);
		ctx.style().slider.cursor_size     = nk_vec2(16.5f,21);
		ctx.style().slider.bar_height      = 1;
		
		 progressbar 
		ctx.style().progress.normal = nk_style_item_color(nk_rgb(231,231,231));
		ctx.style().progress.hover = nk_style_item_color(nk_rgb(231,231,231));
		ctx.style().progress.active = nk_style_item_color(nk_rgb(231,231,231));
		ctx.style().progress.cursor_normal = nk_style_item_color(nk_rgb(63,242,93));
		ctx.style().progress.cursor_hover = nk_style_item_color(nk_rgb(63,242,93));
		ctx.style().progress.cursor_active = nk_style_item_color(nk_rgb(63,242,93));
		ctx.style().progress.border_color = nk_rgb(114,116,115);
		ctx.style().progress.padding = nk_vec2(0,0);
		ctx.style().progress.border = 2;
		ctx.style().progress.rounding = 1;
		
		 combo 
		ctx.style().combo.normal = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().combo.hover = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().combo.active = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().combo.border_color = nk_rgb(95,95,95);
		ctx.style().combo.label_normal = nk_rgb(95,95,95);
		ctx.style().combo.label_hover = nk_rgb(95,95,95);
		ctx.style().combo.label_active = nk_rgb(95,95,95);
		ctx.style().combo.border = 1;
		ctx.style().combo.rounding = 1;
		
		 combo button 
		ctx.style().combo.button.normal = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().combo.button.hover = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().combo.button.active = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().combo.button.text_background = nk_rgb(216,216,216);
		ctx.style().combo.button.text_normal = nk_rgb(95,95,95);
		ctx.style().combo.button.text_hover = nk_rgb(95,95,95);
		ctx.style().combo.button.text_active = nk_rgb(95,95,95);
		
		 property 
		ctx.style().property.normal = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().property.hover = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().property.active = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().property.border_color = nk_rgb(81,81,81);
		ctx.style().property.label_normal = nk_rgb(95,95,95);
		ctx.style().property.label_hover = nk_rgb(95,95,95);
		ctx.style().property.label_active = nk_rgb(95,95,95);
		ctx.style().property.sym_left = NK_SYMBOL_TRIANGLE_LEFT;
		ctx.style().property.sym_right = NK_SYMBOL_TRIANGLE_RIGHT;
		ctx.style().property.rounding = 10;
		ctx.style().property.border = 1;
		
		 edit 
		ctx.style().edit.normal = nk_style_item_color(nk_rgb(240,240,240));
		ctx.style().edit.hover = nk_style_item_color(nk_rgb(240,240,240));
		ctx.style().edit.active = nk_style_item_color(nk_rgb(240,240,240));
		ctx.style().edit.border_color = nk_rgb(62,62,62);
		ctx.style().edit.cursor_normal = nk_rgb(99,202,255);
		ctx.style().edit.cursor_hover = nk_rgb(99,202,255);
		ctx.style().edit.cursor_text_normal = nk_rgb(95,95,95);
		ctx.style().edit.cursor_text_hover = nk_rgb(95,95,95);
		ctx.style().edit.text_normal = nk_rgb(95,95,95);
		ctx.style().edit.text_hover = nk_rgb(95,95,95);
		ctx.style().edit.text_active = nk_rgb(95,95,95);
		ctx.style().edit.selected_normal = nk_rgb(99,202,255);
		ctx.style().edit.selected_hover = nk_rgb(99,202,255);
		ctx.style().edit.selected_text_normal = nk_rgb(95,95,95);
		ctx.style().edit.selected_text_hover = nk_rgb(95,95,95);
		ctx.style().edit.border = 1;
		ctx.style().edit.rounding = 2;
		
		 property buttons 
		ctx.style().property.dec_button.normal = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().property.dec_button.hover = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().property.dec_button.active = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().property.dec_button.text_background = nk_rgba(0,0,0,0);
		ctx.style().property.dec_button.text_normal = nk_rgb(95,95,95);
		ctx.style().property.dec_button.text_hover = nk_rgb(95,95,95);
		ctx.style().property.dec_button.text_active = nk_rgb(95,95,95);
		ctx.style().property.inc_button = ctx.style().property.dec_button;
		
		 property edit 
		ctx.style().property.edit.normal = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().property.edit.hover = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().property.edit.active = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().property.edit.border_color = nk_rgba(0,0,0,0);
		ctx.style().property.edit.cursor_normal = nk_rgb(95,95,95);
		ctx.style().property.edit.cursor_hover = nk_rgb(95,95,95);
		ctx.style().property.edit.cursor_text_normal = nk_rgb(216,216,216);
		ctx.style().property.edit.cursor_text_hover = nk_rgb(216,216,216);
		ctx.style().property.edit.text_normal = nk_rgb(95,95,95);
		ctx.style().property.edit.text_hover = nk_rgb(95,95,95);
		ctx.style().property.edit.text_active = nk_rgb(95,95,95);
		ctx.style().property.edit.selected_normal = nk_rgb(95,95,95);
		ctx.style().property.edit.selected_hover = nk_rgb(95,95,95);
		ctx.style().property.edit.selected_text_normal = nk_rgb(216,216,216);
		ctx.style().property.edit.selected_text_hover = nk_rgb(216,216,216);
		
		 chart 
		ctx.style().chart.background = nk_style_item_color(nk_rgb(216,216,216));
		ctx.style().chart.border_color = nk_rgb(81,81,81);
		ctx.style().chart.color = nk_rgb(95,95,95);
		ctx.style().chart.selected_color = nk_rgb(255,0,0);
		ctx.style().chart.border = 1;*/
	}
}
