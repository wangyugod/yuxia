$(function(){
    // --- Initialize Area trees
    $("#tree").dynatree({
        title: "Area Trees",
        checkbox:true,
        selectMode:1,
        fx: { height: "toggle", duration: 200 },
        autoFocus: false,

        onSelect: function(select, node) {
            // Display list of selected nodes
            var selNodes = node.tree.getSelectedNodes();
            // convert to title/key array
            var selKeys = $.map(selNodes, function(node){
                return node.data.key;
            });
            var selTitle = $.map(selNodes, function(node){
                return node.data.title;
            });
            $("#areaName").val(selTitle.join(","));
            $("#areaId").val(selKeys.join(";"));
        },

        initAjax: {
            url: "/area/root",
            data: { mode: "funnyMode" }
        },

        onActivate: function(node) {
            $("#echoActive").text("" + node + " (" + node.getKeyPath()+ ")");
        },

        onLazyRead: function(node){
            node.appendAjax({
                url: "/area/child/" + node.data.key,
                data: {key: node.data.key, mode: "funnyMode"}
            });
        }
    });
    //auto complete part
    $( "#areaName" ).autocomplete({
        source:"/area/search",
        minLength: 1,
        appendTo: "#popForm",
        select: function(event, ui){
            event.preventDefault();
            $('#areaId').val(ui.item.value)
            $('#areaName').val(ui.item.label);
        },
        focus: function(event, ui) {
            event.preventDefault();
            var menu = $(this).data("uiAutocomplete").menu.element,
                focused = menu.find("li:has(a.ui-state-focus)");
            focused.attr("title", ui.item.detail);
        }
    });
});

function showAreaTree(){
    $("#tree").dialog({
        title: "选择所属区域",
        width: 760,
        height: 400,
        modal:true,
        buttons:{"确定": function(){
        $(this).dialog("close");
    },
        "退出": function() {
            $(this).dialog( "close" );
        }}
})
}