SZ = 20
angle = north: 0, east: 90, south: 180, west: 270

$.fn.addPlayer = (data) ->
	$('<div class="player"><div style="-webkit-transform: rotate('+angle[data.direction]+'deg); ">&uarr;</div></div>')
		.css
			bottom: SZ * data.x
			left: SZ * data.y
		.appendTo(@)

x=0
main = () ->
	$('.board').empty()
	  .addPlayer
		  x:x
		  y:3
		  direction: "north"

$ ->
	$('.actions li').click (ev) ->
		x++
		ev.preventDefault()
	setInterval main, 50
