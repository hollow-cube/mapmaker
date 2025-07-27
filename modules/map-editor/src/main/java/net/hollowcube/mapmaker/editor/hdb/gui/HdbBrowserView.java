package net.hollowcube.mapmaker.editor.hdb.gui;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.editor.hdb.HeadDatabase;
import net.hollowcube.mapmaker.editor.hdb.HeadInfo;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class HdbBrowserView extends View {
    private static final String DEFAULT_CATEGORY = "alphabet";

    private static final String ALPHABET_CATEGORY = "alphabet";
    private static final LinkedHashMap<String, String> ALPHABET_SUBCATEGORIES = new LinkedHashMap<>();

    private @ContextObject HeadDatabase hdb;

    private @Outlet("title") Text titleText;
    private @Outlet("heads") Pagination headsPagination;
    private @Outlet("hdb_browser_title") Text hdbBrowserTitle; //TODO make these match the translation keys without creating aids

    private String category;
    private String subCategory; // Only used for alphabet

    public HdbBrowserView(@NotNull Context context) {
        this(context, DEFAULT_CATEGORY);
        titleText.setText("Head Database");

        hdbBrowserTitle.setText(formatCategoryName(category));
        hdbBrowserTitle.setArgs(Component.text(formatCategoryName(category)));
    }

    private String formatCategoryName(String category) {
        return Arrays.stream(category.replace("-", " & ").split(" "))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    public HdbBrowserView(@NotNull Context context, @NotNull String category) {
        super(context);
        this.category = category;
    }

    @Action(value = "heads", async = true)
    private void createHeadsList(@NotNull Pagination.PageRequest<HeadIconView> request) {
        var heads = subCategory != null ? hdb.getSuggestions(subCategory, request.page(), request.pageSize())
                : hdb.getInCategory(category, request.page(), request.pageSize());
        if (heads.isEmpty()) {
            request.respond(List.of(new HeadIconView(request.context())), false);
            return;
        }

        // Gross special handling for the alphabet category, but oh well.
        if (ALPHABET_CATEGORY.equals(category)) {
            if (subCategory == null) {
                var icons = new ArrayList<HeadInfo>();
                for (var entry : ALPHABET_SUBCATEGORIES.sequencedEntrySet()) {
                    icons.add(new HeadInfo("0", entry.getKey(), "__alphabet", entry.getValue(), List.of()));
                }
                for (int i = 0; i < request.page() * request.pageSize(); i++) {
                    if (icons.isEmpty()) break;
                    icons.removeFirst();
                }
                heads = icons;
            } else {
                // Otherwise we need to filter the heads
                var newHeads = new ArrayList<HeadInfo>();
                for (var head : heads) {
                    if (!head.tags().contains(subCategory)) continue;
                    newHeads.add(head);
                }
                heads = newHeads;
            }
        }

        var result = new ArrayList<HeadIconView>();
        for (int i = 0; i < request.pageSize() && heads.size() > i; i++) {
            result.add(new HeadIconView(request.context(), heads.get(i)));
        }
        request.respond(result, heads.size() > request.pageSize());
    }

    @Action("prev_page")
    private void handlePrevPage() {
        headsPagination.prevPage();
    }

    @Action("next_page")
    private void handleNextPage() {
        headsPagination.nextPage();
    }

    @Action("categories_row_1")
    private void createCategoryList1(@NotNull Pagination.PageRequest<CategoryIconView> request) {
        var result = new ArrayList<CategoryIconView>();
        var categories = new ArrayList<>(hdb.categories());
        int limit = Math.min(categories.size(), 7);  // limit to 7 categories for the 1st row
        for (int i = 0; i < limit; i++) {
            result.add(new CategoryIconView(request.context(), hdb, categories.get(i)));
        }
        request.respond(result, false);
    }

    @Action("categories_row_2")
    private void createCategoryList2(@NotNull Pagination.PageRequest<CategoryIconView2> request) {
        var result = new ArrayList<CategoryIconView2>();
        var categories = new ArrayList<>(hdb.categories());
        int start = Math.max(0, categories.size() - 3); //get the last 3 categories and limit it to that for the 2nd row
        for (int i = start; i < categories.size(); i++) {
            result.add(new CategoryIconView2(request.context(), hdb, categories.get(i)));
        }
        request.respond(result, false);
    }

    @Action("hdb_browser_search")
    private void openSearchMenu() {
        pushView(context -> new HdbSearchView(context, "")); //TODO make the back button work in this UI
    }

    @Signal(CategoryIconView.SIG_SELECTED)
    private void handleCategoryChanged(@NotNull String newCategory) {
        if (newCategory.contains("|")) {
            var split = newCategory.split("\\|");
            this.category = split[0];
            this.subCategory = split[1];
        } else {
            this.category = newCategory;
            this.subCategory = null;
        }
        hdbBrowserTitle.setText(formatCategoryName(category));
        hdbBrowserTitle.setArgs(Component.text(formatCategoryName(category)));
        this.headsPagination.reset();
    }

    static {
        ALPHABET_SUBCATEGORIES.put("Font (Oak)", "a67d813ae7ffe5be951a4f41f2aa619a5e3894e85ea5d4986f84949c63d7672e");
        ALPHABET_SUBCATEGORIES.put("Font (Cleanstone)", "2ac58b1a3b53b9481e317a1ea4fc5eed6bafca7a25e741a32e4e3c2841278c");
        ALPHABET_SUBCATEGORIES.put("Font (Monitor 2)", "dcdb80175169613280a8a06a1c751fb71b6689edc8ce3d8b9d96bd450cc81");
        ALPHABET_SUBCATEGORIES.put("Font (Gold)", "9e5bb8b31f46aa9af1baa88b74f0ff383518cd23faac52a3acb96cfe91e22ebc");
        ALPHABET_SUBCATEGORIES.put("Font (Black)", "3a5d29ce63cd10d539be2595ec5d5b27f7738bf17f481b6ab481d245a32067");
        ALPHABET_SUBCATEGORIES.put("Font (Blue)", "bfa9de39c793528e15de617f429f74bfb43609b524881c494e2fe7add45a67");
        ALPHABET_SUBCATEGORIES.put("Font (Cobblestone)", "e547d18ef6a2c2eba281a94eebb8e979c47938d9d5477b973a52a1de8734e2");
        ALPHABET_SUBCATEGORIES.put("Font (Yellow)", "103da2ed6f1b9cde85218e2780a1f9223dc638c8810fbaad1ae77d6feae7ad7");
        ALPHABET_SUBCATEGORIES.put("Font (White)", "5463d366534d4176f4ec159385d63491d82f29f9f07323b877627bea3dc75d");
        ALPHABET_SUBCATEGORIES.put("Font (Red)", "b837f3db13a40d4979de77179e18af6e0bc3cc39ea6aba518bb080a6f01a40");
        ALPHABET_SUBCATEGORIES.put("Font (Purple)", "8e5aa91f11a27b5cf62aa06dc1d3a47a748bf1870dceda8b6674a301a284");
        ALPHABET_SUBCATEGORIES.put("Font (Pink)", "c8cf6f2fa2a86a35d39c0b7886e9ea1683af8f6662c46af58dd71c3d8a39");
        ALPHABET_SUBCATEGORIES.put("Font (Orange)", "5b64f2ea2ba7e6243bf73a6e3b99a2d14b6348e2ff28d9e8c19bd47f33386a");
        ALPHABET_SUBCATEGORIES.put("Font (Magenta)", "5fee29ba57abbe05ce2c2ce36cc9719db7a5c6a14416488354fe9a842c29");
        ALPHABET_SUBCATEGORIES.put("Font (Lime)", "da481866e6a84f21612eff2902979e9dd885f8018d9e0c4904b4db73d2ea647");
        ALPHABET_SUBCATEGORIES.put("Font (Light Gray)", "4dc8e7cc87d8ff30398fc514e45c78b2573533ee3b8ceba152fdba5183a83");
        ALPHABET_SUBCATEGORIES.put("Font (Light Blue)", "d2e03c5d5c1779e7ce2d55a15412a28c9bc672774ea31c9e846a5bb28466");
        ALPHABET_SUBCATEGORIES.put("Font (Green)", "78432607e86b02ea3caaf9357066ca3f5e4edeef7be9edcbb5e45711c6557");
        ALPHABET_SUBCATEGORIES.put("Font (Gray)", "fbefc867d847ab95e775e04aa5383b1670d38420a827b162d898b8f7ec148ec");
        ALPHABET_SUBCATEGORIES.put("Font (Cyan)", "124a8aac165d7ae78ce5a14f948f1c66f8a27c8a5921fb57184a25ff4cc6ca");
        ALPHABET_SUBCATEGORIES.put("Font (Brown)", "c3b97bdff848f67e2bbfa66c5296d684a87ed1fadca366f924d25841621ffea");
        ALPHABET_SUBCATEGORIES.put("Font (Birch)", "76a75e5a9d648d8e98e5e7bab88a9ec5d79c53338d8e1a5d3bf29d2b248f913");
        ALPHABET_SUBCATEGORIES.put("Font (Jungle)", "f074131b47ecc51a6984ac8d687e7f73f9d2c47204b8c5fb53c1dccbf6");
        ALPHABET_SUBCATEGORIES.put("Font (Spruce)", "8ea6848c5931d3967f58c3eda2555c72c716df9a1da3d2dd6bc63c16cd17b5eb");
        ALPHABET_SUBCATEGORIES.put("Font (Dirt)", "b6c47b1aff976e7209c6d85d71422e73c2f69a3719f0f2a969a0c4eb31c619");
        ALPHABET_SUBCATEGORIES.put("Font (Oak Log)", "f11ab366d1c0b9fe3c4685d3c6ac12b566501111f2a704d8378517e37ee26bf");
        ALPHABET_SUBCATEGORIES.put("Font (Quartz)", "09a52cb50992d83c5599fd6e41a6ce99cf7f1e6203611963dc2c2fda0b55583");
        ALPHABET_SUBCATEGORIES.put("Font (Monitor)", "4c242a593928a82a24efdd7af094cbd0625f3091a5be8df52b34f2b58ed25b");
        ALPHABET_SUBCATEGORIES.put("Font (Pumpkin)", "fc6bd7f25ac1328c3f65b1ee023fa6ae2311884b553177f9f24e21464ed3ff2");
        ALPHABET_SUBCATEGORIES.put("Font (Diamond)", "10a6df3879b529a3494a5a9587656c8354af0bdcec67cd8cf7335f3d6a7f");
        ALPHABET_SUBCATEGORIES.put("Font (Lettercube)", "97dd8be7d414bfc37e8396ed79aed0c525dc2eddaaa8d9bb9d492137a5966b4");
        ALPHABET_SUBCATEGORIES.put("Font (GUI)", "4f6dd32657d000921204987488c35d88c194507fc1911d067831274bef37cc0d");
        ALPHABET_SUBCATEGORIES.put("Font (Plush)", "548f777107f301c240f8154fd94e72e8b22da1ee48705a61efbfcf9ee207f92d");
        ALPHABET_SUBCATEGORIES.put("Font (Watermelon)", "b515b0d1a4a9a230549651e6245b2b58539eeece49b686ebe075f392f996c0d3");
        ALPHABET_SUBCATEGORIES.put("Font (Rainbow)", "9a24b0f6c184ff173686c7d128df536d10b7280f8008636a5546f1c777234354");
        ALPHABET_SUBCATEGORIES.put("Font (Blood)", "b61bf6bbc58a7894635d7f5db409e5f9adb4d83b0659b9f7cb756a5c8a2c1703");
        ALPHABET_SUBCATEGORIES.put("Font (Obsidian)", "faa23cbf177ac5f86dccbd1a9f2b5246a1dcb0284e65be4068e3d98b270b7bff");
        ALPHABET_SUBCATEGORIES.put("Font (Ice)", "70cf569b9e03242f6d4f9bc9579deb6ae3362276633ce238fc17096bad3d841f");
        ALPHABET_SUBCATEGORIES.put("Font (Geode)", "6beb7c933375b272741115f51063af2afce3e5bd154150890bf49bda8342e163");
        ALPHABET_SUBCATEGORIES.put("Font (Sculk Sensor)", "dbb35771a50583b36a78b8b42c1164de20cd1cee2c42b6c7caa6fb1b160d8833");
        ALPHABET_SUBCATEGORIES.put("Font (Redstone Block)", "55ff53f7ede6ce5a4f635d2da630dcc319c1911513989432dda1bbdb7034ffc1");
        ALPHABET_SUBCATEGORIES.put("Font (Dripstone)", "7b562b4fbbe72e1e27cbd099532705461c3d18a262f331745a9692010c8711de");
        ALPHABET_SUBCATEGORIES.put("Font (Smooth Sandstone)", "672660e09c0c5ae6e88ea875bcc1791ce10ff8132676ffcf3ed61ad9a3fb9ed5");
        ALPHABET_SUBCATEGORIES.put("Font (Mangrove Planks)", "647178d3640eaa39f371ddaefa431c6376ec4c731408566e0d7b3f45dbaa62f5");
        ALPHABET_SUBCATEGORIES.put("Font (Cherry Planks)", "dc30c406e98fae234967aba50de5d2650573092857a0d6921ee81e23b35b2862");
        ALPHABET_SUBCATEGORIES.put("Font (Chat)", "abd520f8dfe48638f7263d8edbc5eefd40c95c126f7162da31001ab77ff3dcc1");
        ALPHABET_SUBCATEGORIES.put("Font (Netherite)", "36de3314382b210b34e8c1b5d25ae4b75b849046e8ea0ef0e800a328baab5f7e");
        ALPHABET_SUBCATEGORIES.put("Font (Gilded Blackstone)", "1d971af5fddfe8909873f965bc681ed94de4c200592c79f4c758ea9a2b0f9c5b");
        ALPHABET_SUBCATEGORIES.put("Font (Forest Green)", "bdb01a3077562843090e030ee7b4a8634e6fc2d53e4603a34f28f5f2adc3371d");
        ALPHABET_SUBCATEGORIES.put("Font (Emerald)", "cbecff537c65dacee80de0020a18b5fdc1e85d8a399321f3d6dcd34c306d223f");
        ALPHABET_SUBCATEGORIES.put("Font (Iron)", "987444ed6d845690115b38588cf1a2644da9c71e6f31ae24651038cb1d476ea");
        ALPHABET_SUBCATEGORIES.put("Font (Chiseled Sandstone)", "f01cc6c5463b1be23a05e8999feaf7b804e3ac88573cc858ff9e90d37ca613c7");
    }
}
