(function() {
  var SZ, angle, main, x;
  SZ = 20;
  angle = {
    north: 0,
    east: 90,
    south: 180,
    west: 270
  };
  $.fn.addPlayer = function(data) {
    return $('<div class="player"><div style="-webkit-transform: rotate(' + angle[data.direction] + 'deg); ">&uarr;</div></div>').css({
      bottom: SZ * data.x,
      left: SZ * data.y
    }).appendTo(this);
  };
  x = 0;
  main = function() {
    return $('.board').empty().addPlayer({
      x: x,
      y: 3,
      direction: "north"
    });
  };
  $(function() {
    $('.actions li').click(function(ev) {
      x++;
      return ev.preventDefault();
    });
    return setInterval(main, 50);
  });
}).call(this);
