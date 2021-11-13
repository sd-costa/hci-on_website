$(function(){ // on dom ready

$('#cy').cytoscape({
  style: cytoscape.stylesheet()
    .selector('node')
      .css({
        'content': 'data(name)',
		'width': 'data(dim)',
        'height': 'data(dim)',
		'background-color': 'data(color)',
		'font-size': 32,
        'text-wrap': 'wrap',
        'text-valign': 'center',
        'text-halign': 'center',
      })

	  .selector('edge')
      .css({
		'width': 'data(thickness)',
        'target-arrow-shape': 'triangle',
        'opacity': 0.5,
        'curve-style': 'bezier'
      })
	  
	  .selector('node:selected')
      .css({
		'border-width': '3px',
		'border-color': '#606060',
      })
	  
	  .selector('edge:selected')
      .css({
		'content': 'data(weight)',
		'font-size': 24,
		'text-outline-width': 5,
		'text-outline-color': 'white',
        'line-color': 'grey',
        'target-arrow-color': 'grey',
        'opacity': 1
      })
	  
      .selector('.faded')
      .css({
        'opacity': 0.25,
        'text-opacity': 0
      }),
  
  elements: {
nodes: [
  {data: {id:'0', name:'UFO', dim:265, level:1, color:'#99ffff'}},
  {data: {id:'1', name:'HCIO', dim:130, level:2, color:'#99ff99'}},
  {data: {id:'2', name:'SPO', dim:178, level:2, color:'#99ff99'}},
  {data: {id:'3', name:'SySwO', dim:137, level:2, color:'#99ff99'}},
  {data: {id:'4', name:'COM', dim:136, level:2, color:'#99ff99'}},
  {data: {id:'5', name:'EO', dim:138, level:2, color:'#99ff99'}},
  {data: {id:'6', name:'HCIDO', dim:126, level:3, color:'#ffff99'}},
  {data: {id:'7', name:'SDRO', dim:117, level:3, color:'#ffff99'}},
  {data: {id:'8', name:'HCIEO', dim:111, level:3, color:'#ffff99'}},
  {data: {id:'9', name:'RSRO', dim:116, level:3, color:'#ffff99'}},
  {data: {id:'10', name:'HCIDPO', dim:133, level:3, color:'#ffff99'}},
  {data: {id:'11', name:'RRO', dim:121, level:3, color:'#ffff99'}},
  {data: {id:'12', name:'GORO', dim:106, level:3, color:'#ffff99'}},
  {data: {id:'13', name:'RDPO', dim:112, level:3, color:'#ffff99'}},
  {data: {id:'14', name:'DPO', dim:130, level:3, color:'#ffff99'}},
  {data: {id:'15', name:'CPO', dim:110, level:3, color:'#ffff99'}},
  {data: {id:'16', name:'ROoST', dim:151, level:3, color:'#ffff99'}},
  {data: {id:'17', name:'QAPO', dim:120, level:3, color:'#ffff99'}},
  {data: {id:'18', name:'SPMO', dim:121, level:3, color:'#ffff99'}},
  {data: {id:'19', name:'CMPO', dim:147, level:3, color:'#ffff99'}},
  {data: {id:'20', name:'RSMO', dim:148, level:3, color:'#ffff99'}},
], 

edges: [
  {data: {id:'e0', thickness:8, weight:10, source:'1', target:'0'}},
  {data: {id:'e1', thickness:12, weight:16, source:'1', target:'3'}},
  {data: {id:'e2', thickness:3, weight:3, source:'1', target:'4'}},
  {data: {id:'e3', thickness:2, weight:2, source:'1', target:'6'}},
  {data: {id:'e4', thickness:3, weight:3, source:'1', target:'8'}},
  {data: {id:'e5', thickness:1, weight:1, source:'1', target:'9'}},
  {data: {id:'e6', thickness:20, weight:27, source:'2', target:'0'}},
  {data: {id:'e7', thickness:15, weight:19, source:'2', target:'3'}},
  {data: {id:'e8', thickness:8, weight:10, source:'2', target:'5'}},
  {data: {id:'e9', thickness:1, weight:1, source:'2', target:'18'}},
  {data: {id:'e10', thickness:6, weight:8, source:'2', target:'20'}},
  {data: {id:'e11', thickness:9, weight:12, source:'3', target:'0'}},
  {data: {id:'e12', thickness:2, weight:2, source:'3', target:'1'}},
  {data: {id:'e13', thickness:1, weight:1, source:'3', target:'7'}},
  {data: {id:'e14', thickness:5, weight:6, source:'3', target:'9'}},
  {data: {id:'e15', thickness:1, weight:1, source:'3', target:'20'}},
  {data: {id:'e16', thickness:20, weight:26, source:'4', target:'0'}},
  {data: {id:'e17', thickness:9, weight:11, source:'5', target:'0'}},
  {data: {id:'e18', thickness:1, weight:1, source:'5', target:'2'}},
  {data: {id:'e19', thickness:1, weight:1, source:'5', target:'17'}},
  {data: {id:'e20', thickness:3, weight:4, source:'6', target:'1'}},
  {data: {id:'e21', thickness:3, weight:3, source:'6', target:'3'}},
  {data: {id:'e22', thickness:11, weight:14, source:'6', target:'7'}},
  {data: {id:'e23', thickness:2, weight:2, source:'6', target:'9'}},
  {data: {id:'e24', thickness:3, weight:3, source:'7', target:'0'}},
  {data: {id:'e25', thickness:1, weight:1, source:'7', target:'2'}},
  {data: {id:'e26', thickness:5, weight:6, source:'7', target:'3'}},
  {data: {id:'e27', thickness:2, weight:2, source:'7', target:'9'}},
  {data: {id:'e28', thickness:4, weight:5, source:'8', target:'0'}},
  {data: {id:'e29', thickness:4, weight:5, source:'8', target:'1'}},
  {data: {id:'e30', thickness:2, weight:2, source:'8', target:'2'}},
  {data: {id:'e31', thickness:2, weight:2, source:'8', target:'3'}},
  {data: {id:'e32', thickness:3, weight:3, source:'8', target:'4'}},
  {data: {id:'e33', thickness:1, weight:1, source:'8', target:'6'}},
  {data: {id:'e34', thickness:1, weight:1, source:'8', target:'9'}},
  {data: {id:'e35', thickness:5, weight:6, source:'9', target:'0'}},
  {data: {id:'e36', thickness:3, weight:3, source:'9', target:'2'}},
  {data: {id:'e37', thickness:3, weight:4, source:'9', target:'3'}},
  {data: {id:'e38', thickness:7, weight:9, source:'9', target:'13'}},
  {data: {id:'e39', thickness:5, weight:6, source:'10', target:'2'}},
  {data: {id:'e40', thickness:1, weight:1, source:'10', target:'3'}},
  {data: {id:'e41', thickness:6, weight:7, source:'11', target:'3'}},
  {data: {id:'e42', thickness:2, weight:2, source:'11', target:'9'}},
  {data: {id:'e43', thickness:2, weight:2, source:'12', target:'0'}},
  {data: {id:'e44', thickness:1, weight:1, source:'12', target:'9'}},
  {data: {id:'e45', thickness:6, weight:7, source:'13', target:'2'}},
  {data: {id:'e46', thickness:3, weight:3, source:'13', target:'3'}},
  {data: {id:'e47', thickness:6, weight:7, source:'13', target:'9'}},
  {data: {id:'e48', thickness:12, weight:15, source:'14', target:'2'}},
  {data: {id:'e49', thickness:6, weight:7, source:'14', target:'3'}},
  {data: {id:'e50', thickness:3, weight:3, source:'14', target:'9'}},
  {data: {id:'e51', thickness:6, weight:7, source:'15', target:'2'}},
  {data: {id:'e52', thickness:3, weight:4, source:'15', target:'3'}},
  {data: {id:'e53', thickness:2, weight:2, source:'15', target:'9'}},
  {data: {id:'e54', thickness:2, weight:2, source:'15', target:'14'}},
  {data: {id:'e55', thickness:11, weight:14, source:'16', target:'2'}},
  {data: {id:'e56', thickness:11, weight:14, source:'16', target:'3'}},
  {data: {id:'e57', thickness:10, weight:13, source:'17', target:'2'}},
  {data: {id:'e58', thickness:3, weight:4, source:'17', target:'3'}},
  {data: {id:'e59', thickness:3, weight:3, source:'17', target:'18'}},
  {data: {id:'e60', thickness:11, weight:14, source:'18', target:'2'}},
  {data: {id:'e61', thickness:2, weight:2, source:'18', target:'3'}},
  {data: {id:'e62', thickness:1, weight:1, source:'18', target:'17'}},
  {data: {id:'e63', thickness:2, weight:2, source:'19', target:'0'}},
  {data: {id:'e64', thickness:16, weight:21, source:'19', target:'2'}},
  {data: {id:'e65', thickness:6, weight:7, source:'19', target:'3'}},
  {data: {id:'e66', thickness:2, weight:2, source:'19', target:'17'}},
  {data: {id:'e67', thickness:3, weight:4, source:'20', target:'0'}},
  {data: {id:'e68', thickness:13, weight:17, source:'20', target:'2'}},
  {data: {id:'e69', thickness:6, weight:8, source:'20', target:'3'}},
  {data: {id:'e70', thickness:11, weight:14, source:'20', target:'4'}},
  {data: {id:'e71', thickness:1, weight:1, source:'20', target:'5'}},
]
  },
  
  layout: {
	name: 'concentric',
	clockwise: false,
	minNodeSpacing: 40,
	concentric: function(){
	  return (4-this.data('level'))*10;
	},
  },
  
  ready: function(){
    window.cy = this;
  }
  
});

}); // on dom ready