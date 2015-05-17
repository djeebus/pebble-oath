#include <pebble.h>


static Window *window;
static MenuLayer *menu_layer;


enum {
	OATH_KEY_FETCH = 0,
	OATH_KEY_APPEND = 1,
	OATH_KEY_CODE = 2,
	OATH_KEY_EXPIRATION = 3,
	OATH_KEY_NAME = 4,
};


#define MAX_ITEMS 10
#define MAX_ITEM_TEXT_LENGTH 100
#define MAX_CODE_LENGTH 6


typedef struct {
	uint8_t id;
	char text[MAX_ITEM_TEXT_LENGTH];
	char code[MAX_CODE_LENGTH + 1];
} OathListItem;


static OathListItem s_items[MAX_CODE_LENGTH];
static int s_item_count = 0;


#define MAX_LIST_ITEMS (10)

static OathListItem* get_oath_list_item_at_index(int index) {
	if (index < 0 || index >= MAX_ITEMS) {
		APP_LOG(APP_LOG_LEVEL_WARNING, "invalid list index");
		return NULL;
	}

	return &s_items[index];
}


static int16_t get_cell_height_callback(struct MenuLayer *menu_layer, MenuIndex *cell_index, void *data) {
	return 44;
}


static uint16_t get_num_rows_callback(struct MenuLayer *menu_layer, uint16_t section_index, void *data) {
	return s_item_count;
}


static void draw_row_callback(GContext* ctx, Layer *cell_layer, MenuIndex *cell_index, void *data) {
	OathListItem* item;
	const int index = cell_index->row;

	if ((item = get_oath_list_item_at_index(index)) == NULL) {
		APP_LOG(APP_LOG_LEVEL_INFO, "couldn't find list item at %d", index);
		return;
	}

	menu_cell_basic_draw(ctx, cell_layer, item->text, item->code, NULL);
}


static void select_callback(MenuLayer *menu_layer, MenuIndex *cell_index, void *data) {
	const int index = cell_index->row;
}


static void window_load(Window *window) {
	Layer *window_layer = window_get_root_layer(window);

	GRect window_frame = layer_get_frame(window_layer);

	menu_layer = menu_layer_create(window_frame);
	menu_layer_set_callbacks(menu_layer, NULL, (MenuLayerCallbacks) {
		.get_cell_height = get_cell_height_callback,
		.draw_row = (MenuLayerDrawRowCallback)draw_row_callback,
		.get_num_rows = get_num_rows_callback,
	});
	menu_layer_set_click_config_onto_window(menu_layer, window);
	layer_add_child(window_layer, menu_layer_get_layer(menu_layer));
}


static void oath_list_append(uint id, char *name, char *code) {
	APP_LOG(APP_LOG_LEVEL_INFO, "adding item ... ");
	if (s_item_count == MAX_LIST_ITEMS) {
		APP_LOG(APP_LOG_LEVEL_WARNING, "too many items");
		return;
	}

	s_items[s_item_count].id = id;
	strcpy(s_items[s_item_count].text, name);
	strcpy(s_items[s_item_count].code, code);
	s_item_count++;

	APP_LOG(APP_LOG_LEVEL_INFO, "item added");
}


static void in_received_handler(DictionaryIterator *iter, void *context) {
	Tuple *append_tuple = dict_find(iter, OATH_KEY_APPEND);
	Tuple *name_tuple = dict_find(iter, OATH_KEY_NAME);
	Tuple *code_tuple = dict_find(iter, OATH_KEY_CODE);

  	if (append_tuple && name_tuple) {
    		oath_list_append(
    			append_tuple->value->uint8,
	    		name_tuple->value->cstring,
    			code_tuple->value->cstring);
	} else {
		APP_LOG(APP_LOG_LEVEL_WARNING, "couldn't find tuples");
	}

	menu_layer_reload_data(menu_layer);
}


static void app_message_init() {

	app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);

	uint32_t inbox = app_message_inbox_size_maximum();
	uint32_t outbox = app_message_outbox_size_maximum();

	APP_LOG(APP_LOG_LEVEL_DEBUG_VERBOSE, "app_message_open(%lu,%lu)", inbox, outbox);

	app_message_open(inbox, outbox);

	app_message_register_inbox_received(in_received_handler);
}


static void account_list_init() {

	DictionaryIterator *iter;

	AppMessageResult appMessageResult = app_message_outbox_begin(&iter);
	if (appMessageResult != APP_MSG_OK) {
		APP_LOG(APP_LOG_LEVEL_ERROR, "app_message_outbox_begin == %d", appMessageResult);
		return;
	}

	DictionaryResult dictResult = dict_write_uint8(iter, OATH_KEY_FETCH, 0);
	if (dictResult != DICT_OK) {
		APP_LOG(APP_LOG_LEVEL_ERROR, "dict_write_uint8 == %d", dictResult);
		return;
	}

	APP_LOG(APP_LOG_LEVEL_INFO, "sending init message");
	app_message_outbox_send();
}


static void window_appear() {
	APP_LOG(APP_LOG_LEVEL_INFO, "window_appear");
	s_item_count = 0;

	account_list_init();
}


int main() {
	// prepare app
	window = window_create();
	app_message_init();

	window_set_window_handlers(window, (WindowHandlers) {
		.load = window_load,
		.appear = window_appear,
	});
	window_stack_push(window, true);

	// run app
	APP_LOG(APP_LOG_LEVEL_INFO, "running app");
	app_event_loop();

	// clean up app
	window_destroy(window);
	menu_layer_destroy(menu_layer);
}
