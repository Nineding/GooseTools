(function () {
  const search = document.getElementById("role-search");
  const cards = document.querySelectorAll(".role-card");
  const headings = document.querySelectorAll(".faction-heading");
  const filterButtons = document.querySelectorAll("[data-filter]");
  const roleCount = document.getElementById("role-count");
  const roleTotal = document.getElementById("role-total");
  const emptyState = document.getElementById("role-empty");
  let faction = "all";
  let total = 0;

  cards.forEach(function () {
    total += 1;
  });

  if (roleTotal) {
    roleTotal.setText(total.toString());
  }

  function compactSearchText(value) {
    return value.toLowerCase()
      .split(" ").join("")
      .split("-").join("")
      .split("_").join("");
  }

  function updateRoles() {
    if (!search) return;
    const query = compactSearchText(search.getValue());
    const visibleByFaction = { good: 0, evil: 0, neutral: 0 };
    let visible = 0;
    cards.forEach(function (card) {
      const cardFaction = card.getAttribute("data-faction");
      const factionMatches = faction === "all" || cardFaction === faction;
      const visibleText = compactSearchText(card.getText());
      const searchAliases = compactSearchText(card.getAttribute("data-search"));
      const textMatches = query.length === 0
        || visibleText.indexOf(query) >= 0
        || searchAliases.indexOf(query) >= 0;
      const shown = factionMatches && textMatches;
      card.setHidden(!shown);
      if (shown) {
        visible += 1;
        visibleByFaction[cardFaction] += 1;
      }
    });
    headings.forEach(function (heading) {
      heading.setHidden(visibleByFaction[heading.getAttribute("data-faction")] === 0);
    });
    if (roleCount) roleCount.setText(visible.toString());
    if (emptyState) emptyState.setHidden(visible !== 0);
  }

  if (search) {
    search.addEventListener("input", updateRoles);
    filterButtons.forEach(function (button) {
      button.addEventListener("click", function () {
        faction = button.getAttribute("data-filter");
        filterButtons.forEach(function (item) {
          item.removeClass("filter-active");
        });
        button.addClass("filter-active");
        updateRoles();
      });
    });
    updateRoles();
  }
  console.log("GooseTools Web UI ready");
})();
